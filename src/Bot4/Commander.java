package Bot4;

import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Commander extends Robot {
    RobotController rc;
    int id;

    public Commander(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;

        findSymmetry();
    }

    void play() throws GameActionException {
        // do some kind of big brain stuff or smth idk
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        if (rc.getRoundNum() == 751) rc.buyGlobal(GlobalUpgrade.HEALING);
        else if (rc.getRoundNum() == 1501) rc.buyGlobal(GlobalUpgrade.HEALING);
    }

    @Override
    void endTurn() throws GameActionException {

    }

    private void findSymmetry() throws GameActionException {
        int write = rc.readSharedArray(0);
        MapLocation[] announced = rc.senseBroadcastFlagLocations();
        MapLocation[] flags = {MapHelper.poseDecoder(rc.readSharedArray(1)),
                                MapHelper.poseDecoder(rc.readSharedArray(2)),
                                MapHelper.poseDecoder(rc.readSharedArray(3))};

        if (checkTripose(flags, announced, 0, 0, 1, 2) &&
                checkTripose(flags, announced, 0, 0, 2, 1) &&
                checkTripose(flags, announced, 0, 1, 0, 2) &&
                checkTripose(flags, announced, 0, 1, 2, 0) &&
                checkTripose(flags, announced, 0, 2, 0, 1) &&
                checkTripose(flags, announced, 0, 2, 1, 0)) {
            write |= 0x8000;
        }
        if (checkTripose(flags, announced, 1, 0, 1, 2) &&
                checkTripose(flags, announced, 1, 0, 2, 1) &&
                checkTripose(flags, announced, 1, 1, 0, 2) &&
                checkTripose(flags, announced, 1, 1, 2, 0) &&
                checkTripose(flags, announced, 1, 2, 0, 1) &&
                checkTripose(flags, announced, 1, 2, 1, 0)) {
            write |= 0x4000;
        }
        if (checkTripose(flags, announced, 2, 0, 1, 2) &&
                checkTripose(flags, announced, 2, 0, 2, 1) &&
                checkTripose(flags, announced, 2, 1, 0, 2) &&
                checkTripose(flags, announced, 2, 1, 2, 0) &&
                checkTripose(flags, announced, 2, 2, 0, 1) &&
                checkTripose(flags, announced, 2, 2, 1, 0)) {
            write |= 0x2000;
        }

        rc.writeSharedArray(0, write);
    }

    // checks whether the matching flag-comparisons work with the given symmetry
    private boolean checkTripose(MapLocation[] flags, MapLocation[] announced, int check, int _0, int _1, int _2) {
        MapLocation[] modulated = {null, null, null};

        switch (check) {
            case 0: { // vertical
                modulated[0] = reflectVertically(flags[0]);
                modulated[1] = reflectVertically(flags[1]);
                modulated[2] = reflectVertically(flags[2]);
                break;
            }
            case 1: { // horizontal
                modulated[0] = reflectHorizontally(flags[0]);
                modulated[1] = reflectHorizontally(flags[1]);
                modulated[2] = reflectHorizontally(flags[2]);
                break;
            }
            default: { // rotational
                modulated[0] = rotate(flags[0]);
                modulated[1] = rotate(flags[1]);
                modulated[2] = rotate(flags[2]);
                break;
            }
        }

        return !announced[_0].isWithinDistanceSquared(modulated[0], 100) ||
               !announced[_1].isWithinDistanceSquared(modulated[1], 100) ||
               !announced[_2].isWithinDistanceSquared(modulated[2], 100);
    }

    private boolean checkValidity(MapLocation flag, MapLocation[] announced, int check) {
        switch(check) {
            case 0: { // vertical symmetry
                MapLocation verticalFlip = reflectVertically(flag);
                if (announced[0].isWithinDistanceSquared(verticalFlip, 100)) return true;
                if (announced[1].isWithinDistanceSquared(verticalFlip, 100)) return true;
                if (announced[2].isWithinDistanceSquared(verticalFlip, 100)) return true;
                break;
            }
            case 1: { // horizontal symmetry
                MapLocation horizontalFlip = reflectHorizontally(flag);
                if (announced[0].isWithinDistanceSquared(horizontalFlip, 100)) return true;
                if (announced[1].isWithinDistanceSquared(horizontalFlip, 100)) return true;
                if (announced[2].isWithinDistanceSquared(horizontalFlip, 100)) return true;
                break;
            }
            default: { // rotational symmetry
                MapLocation rotated = reflectHorizontally(flag);
                if (announced[0].isWithinDistanceSquared(rotated, 100)) return true;
                if (announced[1].isWithinDistanceSquared(rotated, 100)) return true;
                if (announced[2].isWithinDistanceSquared(rotated, 100)) return true;
                break;
            }
        }

        return false;
    }

    private MapLocation reflectVertically(MapLocation original) {
        return new MapLocation(original.x, rc.getMapHeight() - original.y - 1);
    }

    private MapLocation reflectHorizontally(MapLocation original) {
        return new MapLocation(rc.getMapWidth() - original.x - 1, original.y);
    }

    private MapLocation rotate(MapLocation original) {
        return new MapLocation(rc.getMapWidth() - original.x - 1, rc.getMapHeight() - original.y - 1);
    }
}
