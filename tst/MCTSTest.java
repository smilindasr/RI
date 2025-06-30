import main.MCTS;
import org.junit.Test;

public class MCTSTest {

    @Test
    public void rewardTest() {
        MCTS<Integer> mcts = new MCTS<>();
        double reward = mcts.calculateReward(0.0, 1, 5);
        System.out.println(String.format("%s reward with %s basereward %s depth and %s maxDepth", reward, 1, 1, 5));
        reward = mcts.calculateReward(0.0, 1, 1);
        System.out.println(String.format("%s reward with %s basereward %s depth and %s maxDepth", reward, 1, 1, 1));
    }
}