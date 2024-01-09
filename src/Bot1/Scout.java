package Bot1;

import battlecode.common.*;

public class Scout extends Robot {
    RobotController rc;
    MapLocation curDest;

    public Scout(RobotController rc) throws GameActionException {
        super(rc);
        this.rc = rc;

        curDest = new MapLocation(0, rc.getMapHeight()-1);
    }

    void play() throws GameActionException {

    }

    @Override
    void initTurn() throws GameActionException {

    }

    @Override
    void endTurn() throws GameActionException {

    }

    @Override
    public int getNextSpawnableLocation(MapLocation[] spawns, int id) {
        double minDist = Integer.MAX_VALUE;
        int minDistID = -1;

        for (int i=spawns.length-1; i>=0; i--) {
            if (rc.canSpawn(spawns[i]) && spawns[i].distanceSquaredTo(curDest) < minDist) {
                minDist = spawns[i].distanceSquaredTo(curDest);
                minDistID = i;
            }
        }

        System.out.println(minDistID);
        return minDistID;
    }
}
