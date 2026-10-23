package yio.tro.opacha.game.gameplay.model;

import java.util.*;

/** Travel fairness only. Garrisons, production and contested passages still affect a real race. */
public final class FlagPlacement {
    private FlagPlacement() { }

    public static double[] travelDistances(Planet start, List<Planet> nodes) {
        Map<Planet, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < nodes.size(); i++) indices.put(nodes.get(i), i);
        double[] distance = new double[nodes.size()];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        boolean[] visited = new boolean[nodes.size()];
        distance[indices.get(start)] = 0;
        for (int step = 0; step < nodes.size(); step++) {
            int at = -1;
            for (int i = 0; i < nodes.size(); i++) if (!visited[i]
                    && (at < 0 || distance[i] < distance[at])) at = i;
            if (at < 0 || !Double.isFinite(distance[at])) break;
            visited[at] = true;
            Planet current = nodes.get(at);
            for (Link link : current.adjoinedLinks) {
                if (link.isBlueprint()) continue;
                Integer next = indices.get(link.getOppositePlanet(current));
                if (next == null) continue;
                double cost = link.distance / link.speedFrom(current);
                distance[next] = Math.min(distance[next], distance[at] + cost);
            }
        }
        return distance;
    }

    public static Planet choose(List<Planet> nodes) {
        Map<FractionType, double[]> arrivals = new EnumMap<>(FractionType.class);
        for (Planet start : nodes) {
            if (start.isNeutral()) continue;
            double[] distances = travelDistances(start, nodes), previous = arrivals.get(start.fraction);
            if (previous == null) arrivals.put(start.fraction, distances);
            else for (int i = 0; i < nodes.size(); i++) previous[i] = Math.min(previous[i], distances[i]);
        }
        Planet best = null; double bestSpread = Double.POSITIVE_INFINITY, bestTotal = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nodes.size(); i++) {
            if (!nodes.get(i).isNeutral() || arrivals.isEmpty()) continue;
            double min = Double.POSITIVE_INFINITY, max = 0, total = 0;
            for (double[] distances : arrivals.values()) {
                min = Math.min(min, distances[i]); max = Math.max(max, distances[i]); total += distances[i];
            }
            if (!Double.isFinite(total) || min <= 0) continue;
            double spread = (max - min) / max;
            if (spread < bestSpread - 1e-9 || (Math.abs(spread - bestSpread) <= 1e-9 && total < bestTotal)) {
                best = nodes.get(i); bestSpread = spread; bestTotal = total;
            }
        }
        return best;
    }
}
