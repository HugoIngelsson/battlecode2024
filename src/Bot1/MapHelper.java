package Bot1;

import battlecode.common.*;

public class MapHelper {
    static int WIDTH, HEIGHT;

    // returns the corresponding bit for a quadrant on the shared array at index 0
    // total bytecost: 18
    public static int getQuadrant(MapLocation pos) {
        if (pos.x <= WIDTH/2)
            if (pos.y <= HEIGHT/2)
                return 12;
            else
                return 11;
        else
            if (pos.y <= HEIGHT/2)
                return 9;
            else
                return 10;
    }
}
