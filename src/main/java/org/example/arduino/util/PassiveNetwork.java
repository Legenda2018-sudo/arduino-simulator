package org.example.arduino.util;

import java.util.*;
import org.example.arduino.model.*;
import org.example.arduino.model.Component;
import org.example.arduino.model.Timer;

/** DC Thevenin equivalent seen by a single two-wire LED load.
 * Wires retain the application's educational resistance per pixel.
 * Other LEDs and other voltage sources are open circuits in this calculation.
 */
final class PassiveNetwork {
    private record Terminal(Object component, int pin) { }
    private record Edge(int a, int b, double r, boolean wire) { }
    record Equivalent(double volts, double ohms, double wireOhms) { }
    private final Map<Terminal,Integer> ids = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Component,List<Wire>> incident = new IdentityHashMap<>();
    private final LED target;
    private final Component source;
    private final List<Wire> loadWires;
    private boolean invalid;

    private PassiveNetwork(LED target, Component source, List<Wire> wires) {
        this.target = target; this.source = source;
        Set<Component> connected = new HashSet<>();
        Queue<Component> queue = new ArrayDeque<>();
        connected.add(target); queue.add(target);
        while (!queue.isEmpty()) {
            Component current = queue.remove();
            for (Wire wire : wires) {
                Component next = wire.getFrom() == current ? wire.getTo() : wire.getTo() == current ? wire.getFrom() : null;
                if (next != null && connected.add(next)) queue.add(next);
            }
        }
        wires = wires.stream().filter(w -> connected.contains(w.getFrom()) || connected.contains(w.getTo())).toList();
        for (Wire w : wires) {
            if (w.getFrom() != null) incident.computeIfAbsent(w.getFrom(), k -> new ArrayList<>()).add(w);
            if (w.getTo() != null && w.getTo() != w.getFrom()) incident.computeIfAbsent(w.getTo(), k -> new ArrayList<>()).add(w);
        }
        loadWires = incident.getOrDefault(target, List.of());
        for (Wire w : wires) {
            if (!allowed(w.getFrom()) || !allowed(w.getTo())) continue;
            int a = endpoint(w.getFromAnchor(), w), b = endpoint(w.getToAnchor(), w);
            if (!Double.isFinite(w.getResistanceOhms()) || w.getResistanceOhms() < 0) invalid = true;
            edges.add(new Edge(a, b, w.getResistanceOhms(),true));
        }
        for (Component c : incident.keySet()) {
            if (c instanceof Resistor resistor) {
                edges.add(new Edge(id(c,1),id(c,2),resistor.getResistance(),false));
            }
            if (c instanceof ArduinoUNO uno && c != source && CircuitAnalyzer.controllerClosed(uno)) {
                edges.add(new Edge(id(c,1),id(c,2),0,false));
            }
        }
    }
    private boolean allowed(Component c) {
        if (c == null || c == target || c == source || c instanceof Resistor) return true;
        if (c instanceof org.example.arduino.model.Button b) return b.isPressed();
        if (c instanceof Timer t) return t.getOutput();
        if (c instanceof ArduinoUNO uno) return CircuitAnalyzer.controllerClosed(uno);
        return false;
    }
    private int id(Object c, int p) { return ids.computeIfAbsent(new Terminal(c,p), k -> ids.size()); }
    private int endpoint(WireAnchor a, Wire wire) {
        if (a.isRailPlus()) return id("rail",2);
        if (a.isRailMinus()) return id("rail",1);
        Component c = a.getComponent(); int pin = a.getPin();
        if (c == target) {
            // Old files have one visual LED contact. Only two distinct wires define its two sides.
            if (loadWires.size() != 2) invalid = true;
            return id(c,loadWires.indexOf(wire)+1);
        }
        if (c instanceof Resistor && pin == 0) {
            List<Wire> attached = incident.getOrDefault(c,List.of());
            // Compatibility with old center-to-center series circuits, never guess a branching contact.
            if (attached.size() != 2 || attached.stream().anyMatch(w ->
                (w.getFrom() == c ? w.getFromAnchor() : w.getToAnchor()).getPin() != 0)) invalid = true;
            pin = attached.indexOf(wire)+1;
        }
        if ((c instanceof Resistor || c instanceof ArduinoUNO || c == source) && pin != 1 && pin != 2) invalid = true;
        if (c instanceof org.example.arduino.model.Button || c instanceof Timer) pin = 0;
        return id(c,pin);
    }
    static Equivalent analyze(LED led, Component source, double volts, List<Wire> wires) {
        PassiveNetwork n = new PassiveNetwork(led,source,wires);
        if (n.invalid || n.loadWires.size() != 2) return null;
        int a=n.id(led,1), b=n.id(led,2);
        int plus=n.id(source == null ? "rail" : source,2), minus=n.id(source == null ? "rail" : source,1);
        double[] open = n.solve(Map.of(plus,volts,minus,0.0), Map.of(), a,b);
        if (open == null) return null;
        // Suppress the ideal source, then inject 1 A between LED terminals to obtain Rth.
        n.edges.add(new Edge(plus,minus,0,false));
        double[] resistance=n.solve(Map.of(b,0.0),Map.of(a,1.0),a,b);
        if (resistance == null) return null;
        if (!Double.isFinite(open[a]) || !Double.isFinite(open[b]) || !Double.isFinite(resistance[a]) || !Double.isFinite(resistance[b])) return null;
        double wireOhms=0;
        for(Edge e:n.edges) if(e.wire && e.r>0) wireOhms+=Math.pow(resistance[e.a]-resistance[e.b],2)/e.r;
        return new Equivalent(Math.abs(open[a]-open[b]),Math.max(0,resistance[a]-resistance[b]),wireOhms);
    }
    private double[] solve(Map<Integer,Double> fixed, Map<Integer,Double> injection, int a, int b) {
        int size=ids.size(); int[] parent=new int[size];
        for(int i=0;i<size;i++) parent[i]=i;
        for(Edge e:edges) if(e.r==0) parent[find(parent,e.a)]=find(parent,e.b);
        Map<Integer,Double> bound=new HashMap<>();
        for(var v:fixed.entrySet()) {
            int root=find(parent,v.getKey());
            if(bound.containsKey(root) && Math.abs(bound.get(root)-v.getValue())>1e-9) return null;
            bound.put(root,v.getValue());
        }
        Map<Integer,Set<Integer>> adj=new HashMap<>();
        for(Edge e:edges) {
            int x=find(parent,e.a),y=find(parent,e.b);
            adj.computeIfAbsent(x,k->new HashSet<>()).add(y);
            adj.computeIfAbsent(y,k->new HashSet<>()).add(x);
        }
        Set<Integer> active=new HashSet<>(bound.keySet()); ArrayDeque<Integer> q=new ArrayDeque<>(active);
        while(!q.isEmpty()) for(int next:adj.getOrDefault(q.remove(),Set.of())) if(active.add(next)) q.add(next);
        if(!active.contains(find(parent,a)) || !active.contains(find(parent,b))) return null;
        List<Integer> unknown=active.stream().filter(i->!bound.containsKey(i)).sorted().toList();
        // Bound the dense educational solver so a malformed scheme cannot freeze the UI.
        if(unknown.size()>256) return null;
        Map<Integer,Integer> row=new HashMap<>();
        for(int i=0;i<unknown.size();i++) row.put(unknown.get(i),i);
        int count=unknown.size(); double[][] m=new double[count][count+1];
        for(var v:injection.entrySet()) {
            Integer i=row.get(find(parent,v.getKey())); if(i!=null) m[i][count]+=v.getValue();
        }
        for(Edge e:edges) {
            int x=find(parent,e.a),y=find(parent,e.b);
            if(x==y || !active.contains(x) || !active.contains(y)) continue;
            double g=1/e.r;
            stamp(m,row,bound,x,y,g,count); stamp(m,row,bound,y,x,g,count);
        }
        for(int k=0;k<count;k++) {
            int pivot=k;
            for(int i=k+1;i<count;i++) if(Math.abs(m[i][k])>Math.abs(m[pivot][k])) pivot=i;
            if(Math.abs(m[pivot][k])<1e-14) return null;
            double[] tmp=m[k];m[k]=m[pivot];m[pivot]=tmp;
            double div=m[k][k]; for(int j=k;j<=count;j++) m[k][j]/=div;
            for(int i=0;i<count;i++) if(i!=k) {
                double factor=m[i][k]; for(int j=k;j<=count;j++) m[i][j]-=factor*m[k][j];
            }
        }
        double[] result=new double[size];
        for(int i=0;i<size;i++) {
            int root=find(parent,i);
            result[i]=bound.containsKey(root)?bound.get(root):row.containsKey(root)?m[row.get(root)][count]:0;
        }
        return result;
    }
    private static void stamp(double[][] m,Map<Integer,Integer> row,Map<Integer,Double> fixed,int x,int y,double g,int count) {
        Integer i=row.get(x); if(i==null)return;
        m[i][i]+=g; Integer j=row.get(y);
        if(j!=null)m[i][j]-=g;else m[i][count]+=g*fixed.get(y);
    }
    private static int find(int[] p,int i) {
        while(p[i]!=i){p[i]=p[p[i]];i=p[i];} return i;
    }
}
