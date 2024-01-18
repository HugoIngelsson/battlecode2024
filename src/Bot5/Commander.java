package Bot5;

import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Arrays;
import java.util.Map;

public class Commander extends Robot {
    public static final int DEATH_LIMIT = 20;
    RobotController rc;
    int id;
    int byteThirteen;
    int attackCountdown;

    int[] flagDefensibility;
    MapLocation[] flags;

    public Commander(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;

        this.attackCountdown = 200;
        rc.writeSharedArray(13, 0);

        findSymmetry();
        readProtection();
        getFlagSafeties();

        endTurn();
    }

    void play() throws GameActionException {
        // do some kind of big brain stuff or smth idk
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        if (rc.getRoundNum() == 200) {

        }
        else if (rc.getRoundNum() == 751) rc.buyGlobal(GlobalUpgrade.HEALING);
        else if (rc.getRoundNum() == 1501) rc.buyGlobal(GlobalUpgrade.HEALING);

        this.byteThirteen = rc.readSharedArray(13);
        if (attackCountdown < 0 && byteThirteen > DEATH_LIMIT) {
            attackCountdown = 25;
        }
    }

    @Override
    void endTurn() throws GameActionException {
        // turn on recon bits
        byteZero |= 0x1c00;

        attackCountdown--;
        if (attackCountdown == 0) {
            byteZero -= 0x1c00;

            byteThirteen = 0;
            rc.writeSharedArray(0, byteThirteen);
        }

        rc.writeSharedArray(0, byteZero);
    }

    public void getFlagSafeties() throws GameActionException {
        int byteZero = rc.readSharedArray(0);

        MapLocation[] enemyFlags = new MapLocation[3];
        if (!RobotPlayer.bitAt(byteZero, 15)) {
            enemyFlags[0] = reflectVertically(flags[0]);
            enemyFlags[1] = reflectVertically(flags[1]);
            enemyFlags[2] = reflectVertically(flags[2]);
        }
        else if (!RobotPlayer.bitAt(byteZero, 14)) {
            enemyFlags[0] = reflectHorizontally(flags[0]);
            enemyFlags[1] = reflectHorizontally(flags[1]);
            enemyFlags[2] = reflectHorizontally(flags[2]);
        }
        else {
            enemyFlags[0] = rotate(flags[0]);
            enemyFlags[1] = rotate(flags[1]);
            enemyFlags[2] = rotate(flags[2]);
        }

        MapLocation weakestSpot = null;
        int defensibility = Integer.MAX_VALUE;
        for (int i=0; i<3; i++) {
            int minSafe = enemyFlags[i].distanceSquaredTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
            int minHazard = Integer.MAX_VALUE;
            for (int j=0; j<3; j++) {
                minHazard = Math.min(minHazard, enemyFlags[i].distanceSquaredTo(flags[j]));
            }

            flagDefensibility[i] *= 10;
            flagDefensibility[i] += minHazard + minSafe;

            if (flagDefensibility[i] < defensibility) {
                weakestSpot = enemyFlags[i];
                defensibility = flagDefensibility[i];
            }
        }

        rc.writeSharedArray(4, MapHelper.poseEncoder(weakestSpot));
        rc.writeSharedArray(5, MapHelper.poseEncoder(weakestSpot));
        rc.writeSharedArray(6, MapHelper.poseEncoder(weakestSpot));

        MapLocation closestSpawn = null;
        int distance = Integer.MAX_VALUE;
        for (int i=0; i<3; i++) {
            if (distance > weakestSpot.distanceSquaredTo(flags[i])) {
                distance = weakestSpot.distanceSquaredTo(flags[i]);
                closestSpawn = flags[i];
            }
        }

        int halfwayToTheMiddle = MapHelper.poseEncoder(new MapLocation(
                                        (closestSpawn.x + rc.getMapWidth()/2) / 2,
                                        (closestSpawn.y + rc.getMapHeight()/2) / 2
                                 ));

        rc.writeSharedArray(7, halfwayToTheMiddle);
        rc.writeSharedArray(8, halfwayToTheMiddle);
        rc.writeSharedArray(9, halfwayToTheMiddle);
    }

    private void readProtection() throws GameActionException {
        flagDefensibility = new int[3];
        flagDefensibility[0] = rc.readSharedArray(10);
        flagDefensibility[1] = rc.readSharedArray(11);
        flagDefensibility[2] = rc.readSharedArray(12);

        rc.writeSharedArray(10, 0);
        rc.writeSharedArray(11, 0);
        rc.writeSharedArray(12, 0);
    }

    private void findSymmetry() throws GameActionException {
        int write = rc.readSharedArray(0);
        MapLocation[] announced = rc.senseBroadcastFlagLocations();

        flags = new MapLocation[3];
        flags[0] = MapHelper.poseDecoder(rc.readSharedArray(1));
        flags[1] = MapHelper.poseDecoder(rc.readSharedArray(2));
        flags[2] = MapHelper.poseDecoder(rc.readSharedArray(3));

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
