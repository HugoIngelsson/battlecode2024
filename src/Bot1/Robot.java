package Bot1;

import battlecode.common.*;

public abstract class Robot {
    RobotController rc;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
    }

    abstract void play() throws GameActionException;

    void initTurn() throws GameActionException {}

    void endTurn() throws GameActionException {}

    public int getNextSpawnableLocation(MapLocation[] spawns, int id) {
        for (int i=id; i<27; i++) {
            if (rc.canSpawn(spawns[i])) return i;
        }
        for (int i=id-1; i>=0; i--) {
            if (rc.canSpawn(spawns[i])) return i;
        }

        return -1;
    }
}
