package Bot19;

import battlecode.common.*;

public class Commander extends Robot {
    public static int DEATH_LIMIT = 20;
    RobotController rc;
    Robot internalState;
    int id;
    int byteThirteen;
    int attackCountdown;
    int flagsCaptured;
    boolean[] potentialCaptures;
    boolean shenanigansAfoot;

    int currentTarget;
    int[] flagDefensibility;
    MapLocation[] flags;
    MapLocation[] enemyFlags;

    MapLocation middle;
    MapLocation weakestSpot;

    MapLocation[] startPoints;
    MapLocation[] currentPoints;
    MapLocation[] confirmedFlags;
    int confirmationCount = 0;
    int[] lastRoundSeen;

    public Commander(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;

        this.attackCountdown = 200;
        rc.writeSharedArray(13, 0);

        this.flagsCaptured = 0;
        this.potentialCaptures = new boolean[3];
        this.middle = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        this.shenanigansAfoot = false;

        this.startPoints = new MapLocation[3];
        this.currentPoints = new MapLocation[3];
        this.confirmedFlags = new MapLocation[3];
        this.lastRoundSeen = new int[3];

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
        }

        if (rc.getRoundNum() == 601) rc.buyGlobal(GlobalUpgrade.ATTACK);
        else if (rc.getRoundNum() == 1201) rc.buyGlobal(GlobalUpgrade.HEALING);
        else if (rc.getRoundNum() == 1801) rc.buyGlobal(GlobalUpgrade.CAPTURING);

        if (flagsCaptured == 1 && RobotPlayer.bitAt(byteZero, 8) ||
                flagsCaptured == 0 && RobotPlayer.bitAt(byteZero, 9)) {

            flagsCaptured++;
            System.out.println("Flags captured: " + flagsCaptured);

            whichCaptured();
        }

        int weakestID = nextWeakest(flagDefensibility);
        weakestSpot = enemyFlags[weakestID];

        if (!potentialCaptures[0]) {
            rc.writeSharedArray(4, MapHelper.poseEncoder(enemyFlags[0]));
        }
        else {
            rc.writeSharedArray(4, MapHelper.poseEncoder(weakestSpot));
        }
        if (!potentialCaptures[1]) {
            rc.writeSharedArray(5, MapHelper.poseEncoder(enemyFlags[1]));
        }
        else {
            rc.writeSharedArray(5, MapHelper.poseEncoder(weakestSpot));
        }
        if (!potentialCaptures[2]) {
            rc.writeSharedArray(6, MapHelper.poseEncoder(enemyFlags[2]));
        }
        else {
            rc.writeSharedArray(6, MapHelper.poseEncoder(weakestSpot));
        }

        checkAlert();
        clearFlagTracker();

        this.byteThirteen = rc.readSharedArray(13);
        if (attackCountdown < 0 && byteThirteen > DEATH_LIMIT) {
            attackCountdown = Math.max(rc.getMapWidth(), rc.getMapHeight());
        }

        updateTracker(14);
        updateTracker(16);
        updateTracker(18);
    }

    @Override
    void endTurn() throws GameActionException {
        // turn on recon bits
        byteZero |= 0x1c00;

        attackCountdown--;
        if (attackCountdown == 0) {
            byteZero -= 0x1c00;

            byteThirteen = 0;
            rc.writeSharedArray(13, byteThirteen);
        }

        rc.writeSharedArray(0, byteZero);
    }

    public void getFlagSafeties() throws GameActionException {
        int byteZero = rc.readSharedArray(0);

        enemyFlags = new MapLocation[3];
        if ((byteZero & 0xe000) == 0 || (byteZero & 0xe000) == 0xe000) { // uh oh! couldn't determine any kind of symmetry!
            turtle();
            shenanigansAfoot = true;
            return;
        }
        else if (!RobotPlayer.bitAt(byteZero, 15)) {
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
            flagDefensibility[i] *= 10;
            flagDefensibility[i] += defenseOfSpot(enemyFlags[i]);
        }
    }

    private void turtle() throws GameActionException {
        enemyFlags[0] = flags[0];
        enemyFlags[1] = flags[1];
        enemyFlags[2] = flags[2];

        rc.writeSharedArray(4, MapHelper.poseEncoder(enemyFlags[0]));
        rc.writeSharedArray(5, MapHelper.poseEncoder(enemyFlags[1]));
        rc.writeSharedArray(6, MapHelper.poseEncoder(enemyFlags[2]));

        rc.writeSharedArray(7, MapHelper.poseEncoder(enemyFlags[0]));
        rc.writeSharedArray(8, MapHelper.poseEncoder(enemyFlags[1]));
        rc.writeSharedArray(9, MapHelper.poseEncoder(enemyFlags[2]));
    }

    private void checkAlert() throws GameActionException {
        if (!RobotPlayer.bitAt(byteZero, 7)) {
            rc.writeSharedArray(7, rc.readSharedArray(1));
            rc.writeSharedArray(8, rc.readSharedArray(1));
            rc.writeSharedArray(9, rc.readSharedArray(1));
        }
        else if (!RobotPlayer.bitAt(byteZero, 6)) {
            rc.writeSharedArray(7, rc.readSharedArray(2));
            rc.writeSharedArray(8, rc.readSharedArray(2));
            rc.writeSharedArray(9, rc.readSharedArray(2));
        }
        else if (!RobotPlayer.bitAt(byteZero, 5)) {
            rc.writeSharedArray(7, rc.readSharedArray(3));
            rc.writeSharedArray(8, rc.readSharedArray(3));
            rc.writeSharedArray(9, rc.readSharedArray(3));
        }
        else {
            rc.writeSharedArray(7, rc.readSharedArray(4));
            rc.writeSharedArray(8, rc.readSharedArray(5));
            rc.writeSharedArray(9, rc.readSharedArray(6));
        }
    }

    private void readProtection() throws GameActionException {
        // reads the number of walls near a flag
        flagDefensibility = new int[3];
        flagDefensibility[0] = rc.readSharedArray(10);
        flagDefensibility[1] = rc.readSharedArray(11);
        flagDefensibility[2] = rc.readSharedArray(12);

        rc.writeSharedArray(10, 0);
        rc.writeSharedArray(11, 0);
        rc.writeSharedArray(12, 0);
    }

    private int nextWeakest(int[] flagDefensibility) {
        int id=0;
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

        // check if we think we've made some captures we haven't
        if (broadcast.length + this.flagsCaptured > GameConstants.NUMBER_FLAGS) {
            this.flagsCaptured = GameConstants.NUMBER_FLAGS - broadcast.length;
        }

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

                // if the location is outside of this radius, it's likely being stowed somewhere
                if (mp.distanceSquaredTo(middle) < rc.getMapHeight() * rc.getMapWidth())
                    oddFlag = false;

                safeToClear &= oddFlag;
            }

            safeToClear |= shenanigansAfoot;
        }

        // we can only update our flag info if we've guaranteed some amount of security
        if (safeToClear) {
            potentialCaptures[0] = true;
            potentialCaptures[1] = true;
            potentialCaptures[2] = true;

            boolean[] foundHomeFor = {true, true, true};
            int homeID = 0;
            for (MapLocation mp : broadcast) {
                int minDist = Integer.MAX_VALUE;
                int minID = -1;

                for (int i=0; i<3; i++) {
                    if (mp.distanceSquaredTo(enemyFlags[i]) < minDist &&
                            mp.distanceSquaredTo(enemyFlags[i]) <= 100) {
                        minDist = mp.distanceSquaredTo(enemyFlags[i]);
                        minID = i;
                    }
                }

                if (minID != -1)
                    potentialCaptures[minID] = false;
                else
                    foundHomeFor[homeID] = false;

                homeID++;
            }

            homeID = 0;
            for (MapLocation mp : broadcast) {
                if (!foundHomeFor[homeID++]) {
                    for (int i=0; i<3; i++) {
                        if (potentialCaptures[i]) {
                            enemyFlags[i] = mp;
                            potentialCaptures[i] = false;
                            flagDefensibility[i] = defenseOfSpot(enemyFlags[i]);
                            break;
                        }
                    }
                }
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

        if (announced.length != 3) return;

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

    private int defenseOfSpot(MapLocation loc) {
        int minSafe = loc.distanceSquaredTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        int minHazard = Integer.MAX_VALUE;
        for (int j=0; j<3; j++) {
            minHazard = Math.min(minHazard, loc.distanceSquaredTo(flags[j]));
        }

        return minHazard + minSafe;
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

    private void clearFlagTracker() {
        for (int i=3; --i >= 0;) {
            if (rc.getRoundNum() - lastRoundSeen[i] > 4) {
                lastRoundSeen[i] = 0;
                currentPoints[i] = null;
            }
        }
    }

    private void updateTracker(int flagID) throws GameActionException {
        if (rc.readSharedArray(flagID+1) == 0)
            return;

        MapLocation loc = MapHelper.poseDecoder(rc.readSharedArray(flagID+1));
        rc.writeSharedArray(flagID+1, 0);
        flagID = flagID / 2 - 7;

        if (startPoints[flagID] == null) {
            startPoints[flagID] = loc;
            if (confirmedFlags[flagID] == null) confirmationCount++;
            confirmedFlags[flagID] = loc;

            if (confirmationCount <= 3) {
                replaceClosestConfirm(loc);
                if (confirmationCount == 3) {
                    confirmationCount++;
                    shenanigansAfoot = false;
                }
            }
        }
        currentPoints[flagID] = loc;
        lastRoundSeen[flagID] = rc.getRoundNum();
    }

    private void replaceClosestConfirm(MapLocation loc) {
        int dist = Integer.MAX_VALUE;
        int closestID = -1;

        for (int i=3; --i >= 0;) {
            if (loc.distanceSquaredTo(enemyFlags[i]) < dist) {
                closestID = i;
                dist = loc.distanceSquaredTo(enemyFlags[i]);
            }
        }

        enemyFlags[closestID] = loc;
    }

    private int whichCaptured() {
        int closestToABase = -1;
        int dist = Integer.MAX_VALUE;
        int closestToAStart = -1;
        int dist2 = Integer.MAX_VALUE;

        for (int i=3; --i >= 0;) {
            if (currentPoints[i] != null) {
                int closestFlagDist = Integer.MAX_VALUE;

                for (int j=3; --j >= 0;) {
                    closestFlagDist = Math.min(closestFlagDist, currentPoints[i].distanceSquaredTo(flags[j]));

                    if (startPoints[i].distanceSquaredTo(enemyFlags[j]) < dist2) {
                        dist2 = startPoints[i].distanceSquaredTo(enemyFlags[j]);
                        closestToAStart = j;
                    }
                }

                if (closestFlagDist < dist && !potentialCaptures[closestToAStart]) {
                    closestToABase = closestToAStart;
                    dist = closestFlagDist;
                    System.out.println(startPoints[i] + " " + enemyFlags[closestToAStart]);
                }
            }
        }

        potentialCaptures[closestToABase] = true;
        return closestToABase;
    }
}