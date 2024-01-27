package Bot21;

import battlecode.common.*;

public class MicroAttacker {

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    static int myRange;
    static int myVisionRange;


    static final int RANGE_EXTENDED_LAUNCHER = 26;
    static final int RANGE_LAUNCHER = 16;

    //static double myDPS;
    //static double[] DPS = new double[]{0, 0, 0, 0, 0, 0, 0};
    //static int[] rangeExtended = new int[]{0, 0, 0, 0, 0, 0, 0};

    static final Direction[] dirs = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    final static int MAX_MICRO_BYTECODE_REMAINING = 2000;

    static RobotController rc;

    MicroAttacker(RobotController rc){
        this.rc = rc;
        myRange = 4;
        myVisionRange = 20;
    }

    static int currentRangeExtended;
    static double currentActionRadius;
    static boolean canAttack;
    static MapLocation currentLoc;
    static int currentDMG = 0;

    boolean doMicro(){
        try {
            if (!rc.isMovementReady()) return false;

            shouldPlaySafe = Micro.allyCount < Micro.enemyCount;
            if (!shouldPlaySafe) return false;

            RobotInfo[] units = Micro.enemies;
            if (Micro.enemyCount == 0) return false;
            canAttack = rc.isActionReady();

            alwaysInRange = false;
            if (!canAttack) alwaysInRange = true;

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(dirs[i]);

            for (RobotInfo unit : units) {
                if (unit == null) continue;
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                currentLoc = unit.getLocation();

                currentDMG = Micro.expectedEnemyDamageOutput(unit);
                currentRangeExtended = RANGE_EXTENDED_LAUNCHER;
                currentActionRadius = RANGE_LAUNCHER;

                microInfo[0].updateEnemy();
                microInfo[1].updateEnemy();
                microInfo[2].updateEnemy();
                microInfo[3].updateEnemy();
                microInfo[4].updateEnemy();
                microInfo[5].updateEnemy();
                microInfo[6].updateEnemy();
                microInfo[7].updateEnemy();
                microInfo[8].updateEnemy();
            }

            units = Micro.allies;
            for (RobotInfo unit : units) {
                if (unit == null) continue;
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                currentLoc = unit.getLocation();

                currentDMG = Micro.expectedAllyDamageOutput(unit);
                currentActionRadius = RANGE_LAUNCHER;

                microInfo[0].updateAlly();
                microInfo[1].updateAlly();
                microInfo[2].updateAlly();
                microInfo[3].updateAlly();
                microInfo[4].updateAlly();
                microInfo[5].updateAlly();
                microInfo[6].updateAlly();
                microInfo[7].updateAlly();
                microInfo[8].updateAlly();
            }

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            return apply(bestMicro);

        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    boolean apply(MicroInfo bestMicro) throws GameActionException {
        if (bestMicro.dir == Direction.CENTER) return false;

        if (rc.canMove(bestMicro.dir)) {
            if(bestMicro.dir != Direction.CENTER)
                rc.move(bestMicro.dir);
            return true;
        }
        return false;
    }

    class MicroInfo{
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;

        int canLandHit = 0;

        int launchersAttackRange = 0;

        int launchersVisionRange = 0;

        int possibleEnemyLaunchers = 0;

        int minDistToAlly = INF;

        MapLocation target = null;

        boolean canMove = true;

        public MicroInfo(Direction dir) throws GameActionException {
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;
            minDistanceToEnemy = INF;
        }

        void updateEnemy(){
            if (!canMove) return;
            int dist = currentLoc.distanceSquaredTo(location);
            if (dist < minDistanceToEnemy)  minDistanceToEnemy = dist;
            if (dist <= currentActionRadius) launchersAttackRange++;
            if (dist <= 20) launchersVisionRange++;
            if (dist <= RANGE_EXTENDED_LAUNCHER) possibleEnemyLaunchers++;
            if (dist <= myRange && canAttack){
                canLandHit = 1;
                target = currentLoc;
                //possibleAllies++;
            }
            //if (dist <= RANGE_EXTENDED_LAUNCHER) enemiesTargeting += currentDMG; //TODO carriers?
        }

        void updateAlly(){
            if (!canMove) return;
            int dist = currentLoc.distanceSquaredTo(location);
            if (dist < minDistToAlly) minDistToAlly = dist;
            //if (dist <= 2) alliesTargeting += currentDMG;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo M){

            //if (safe() > M.safe()) return true;
            //if (safe() < M.safe()) return false;

            if (canMove && !M.canMove) return true;
            if (!canMove && M.canMove) return false;

            if (launchersAttackRange - canLandHit < M.launchersAttackRange - M.canLandHit) return true;
            if (launchersAttackRange - canLandHit > M.launchersAttackRange - M.canLandHit) return false;

            if (launchersVisionRange - canLandHit < M.launchersVisionRange - M.canLandHit) return true;
            if (launchersVisionRange - canLandHit > M.launchersVisionRange - M.canLandHit) return false;

            if (canLandHit > M.canLandHit) return true;
            if (canLandHit < M.canLandHit) return false;

            if (minDistToAlly < M.minDistToAlly) return true;
            if (minDistToAlly > M.minDistToAlly) return false;

            if (inRange()) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;
        }
    }

}
