package main;

import java.util.Map;

public class NeuralNetworkOutput<ActionType> {
    public Map<ActionType, Double> policyHead;
    public double valueHead;
}
