package Bot8;

import battlecode.common.*;

import java.util.Arrays;

public class Commander extends Robot {
    public static int DEATH_LIMIT = 20;
    RobotController rc;
    Robot internalState;
    int id;
    int byteThirteen;
    int attackCountdown;
    int flagsCaptured;
    boolean[] potentialCaptures;

    int currentTarget;
    int[] flagDefensibility;
    MapLocation[] flags;
    MapLocation[] enemyFlags;

    public Commander(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;

        this.attackCountdown = 200;
        rc.writeSharedArray(13, 0);

        this.flagsCaptured = 0;
        this.potentialCaptures = new boolean[3];

        findSymmetry();
        readProtection();
        getFlagSafeties();

        this.internalState = new Attacker(rc, id);
        endTurn();
    }

    void play() throws GameActionException {
        if (!rc.isSpawned()){
            internalState.playIfUnspawned();
        }
        else  {
            internalState.initTurn();
            internalState.play();
        }
        internalState.endTurn();
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        if (rc.getRoundNum()%100 == 1) {
            proofRead();

            int weakestID = nextWeakest(flagDefensibility);
            MapLocation weakestSpot = enemyFlags[weakestID];

            rc.writeSharedArray(4, MapHelper.poseEncoder(weakestSpot));
            rc.writeSharedArray(5, MapHelper.poseEncoder(weakestSpot));
            rc.writeSharedArray(6, MapHelper.poseEncoder(weakestSpot));
        }
        else if (rc.getRoundNum() == 200) {
            MapLocation middle = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);

            rc.writeSharedArray(7, MapHelper.poseEncoder(middle));
            rc.writeSharedArray(8, MapHelper.poseEncoder(middle));
            rc.writeSharedArray(9, MapHelper.poseEncoder(middle));
        }
        else if (rc.getRoundNum() == 751) rc.buyGlobal(GlobalUpgrade.HEALING);
        if (rc.getRoundNum() == 1501) rc.buyGlobal(GlobalUpgrade.HEALING);

        if (flagsCaptured == 1 && RobotPlayer.bitAt(byteZero, 8) ||
            flagsCaptured == 0 && RobotPlayer.bitAt(byteZero, 9)) {

            potentialCaptures[this.currentTarget] = true;

            int weakestID = nextWeakest(flagDefensibility);
            MapLocation weakestSpot = enemyFlags[weakestID];
            System.out.println(flagsCaptured + " " + weakestID + " " + weakestSpot);

            rc.writeSharedArray(4, MapHelper.poseEncoder(weakestSpot));
            rc.writeSharedArray(5, MapHelper.poseEncoder(weakestSpot));
            rc.writeSharedArray(6, MapHelper.poseEncoder(weakestSpot));

            flagsCaptured++;
            DEATH_LIMIT += 5;
        }

        this.byteThirteen = rc.readSharedArray(13);
        if (attackCountdown < 0 && byteThirteen > DEATH_LIMIT) {
            attackCountdown = Math.max(rc.getMapWidth(), rc.getMapHeight());
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

        enemyFlags = new MapLocation[3];
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

        for (int i=0; i<3; i++) {
            int minSafe = enemyFlags[i].distanceSquaredTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
            int minHazard = Integer.MAX_VALUE;
            for (int j=0; j<3; j++) {
                minHazard = Math.min(minHazard, enemyFlags[i].distanceSquaredTo(flags[j]));
            }

            flagDefensibility[i] *= 10;
            flagDefensibility[i] += minHazard + minSafe;
        }

        int weakestID = nextWeakest(flagDefensibility);
        MapLocation weakestSpot = enemyFlags[weakestID];

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

    private int nextWeakest(int[] flagDefensibility) {
        int id=-1;
        int defense = Integer.MAX_VALUE;

        for (int i=0; i<3; i++) {
            if (flagDefensibility[i] < defense && !potentialCaptures[i]) {
                id = i;
                defense = flagDefensibility[i];
            }
        }

        this.currentTarget = id;
        return id;
    }

    private void proofRead() {
        MapLocation[] broadcast = rc.senseBroadcastFlagLocations();
        boolean safeToClear = false;

        // we know that all flags yet to be captured are on the map and unseen
        if (broadcast.length + this.flagsCaptured == GameConstants.NUMBER_FLAGS) {
            safeToClear = true;
            // if each sensed flag is within 10 units of an enemy flag spawn, we can
            // (moderately) safely say that that was its spawn location
            // otherwise, we can't gauge what flags are captured and what flags aren't
            for (MapLocation mp : broadcast) {
                boolean oddFlag = false;

                for (MapLocation flag : this.enemyFlags) {
                    if (mp.distanceSquaredTo(flag) <= 100) {
                        oddFlag = true;
                        break;
                    }
                }

                safeToClear &= oddFlag;
            }
        }

        // we can only update our flag info if we've guaranteed some amount of security
        if (safeToClear) {
            potentialCaptures[0] = true;
            potentialCaptures[1] = true;
            potentialCaptures[2] = true;

            for (MapLocation mp : broadcast) {
                int minDist = Integer.MAX_VALUE;
                int minID = -1;

                for (int i=0; i<3; i++) {
                    if (mp.distanceSquaredTo(enemyFlags[i]) < minDist) {
                        minDist = mp.distanceSquaredTo(enemyFlags[i]);
                        minID = i;
                    }
                }

                potentialCaptures[minID] = false;
            }
        }
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
