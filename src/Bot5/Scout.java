package Bot5;

import Bot5.fast.FastMath;
import battlecode.common.*;

public class Scout extends Robot {
    RobotController rc;
    int id;

    public Scout(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.curDest = FastMath.getRandomMapLocation();
        this.curDest = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    }

    void play() throws GameActionException {
        if (rc.hasFlag()) {
            // do something
            curDest = MapHelper.poseDecoder(rc.readSharedArray(1));

            PathFinding.move(curDest);
            return;
        }
        else if (super.getEnemyFlags().length > 0) {
            tempTarget = super.getEnemyFlags()[0].getLocation();

            if (tempTarget.equals(rc.getLocation()))
                if (!rc.canPickupFlag(tempTarget)) tempTarget = null;
                else if (rc.isActionReady()) rc.pickupFlag(tempTarget);
        }
        else if (super.getNearbyEnemies().length > 0 && rc.isActionReady()) { // see if we can attack an enemy
            RobotInfo weakestAttackable = null;

            for (RobotInfo enemy : super.getNearbyEnemies()) {
                if (rc.canAttack(enemy.getLocation())) {
                    if (weakestAttackable == null || weakestAttackable.getHealth() > enemy.getHealth()) {
                        weakestAttackable = enemy;
                    }
                }

                if (enemy.hasFlag()) { // if the enemy has a flag, we want to focus it
                    curDest = enemy.getLocation();

                    if (rc.canAttack(enemy.getLocation())) {
                        weakestAttackable = enemy;
                        break;
                    }
                }
            }

            if (weakestAttackable != null) {
                rc.attack(weakestAttackable.getLocation());
            }
        }
        else if (super.getNearbyAllies().length > 0 && rc.isActionReady()) {
            RobotInfo weakestHealable = null;

            for (RobotInfo ally : super.getNearbyAllies()) {
                if (rc.canHeal(ally.getLocation())) {
                    if (weakestHealable == null || weakestHealable.getHealth() > ally.getHealth()) {
                        weakestHealable = ally;
                    }
                }

                if (ally.hasFlag()) { // if the ally has a flag, we want to defend it
                    tempTarget = ally.getLocation();

                    if (rc.canHeal(ally.getLocation())) {
                        weakestHealable = ally;
                        break;
                    }
                }
            }

            if (weakestHealable != null && weakestHealable.getHealth() < GameConstants.DEFAULT_HEALTH) {
                rc.heal(weakestHealable.getLocation());
            }
        }
        else if (rc.getHealth() < 1000 && rc.isActionReady()) {
            rc.heal(rc.getLocation());
        }

        if (rc.isMovementReady()) {
            if (tempTarget != null) PathFinding.move(tempTarget);
            else PathFinding.move(curDest);
        }
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(spawnLocs, id%27);

        // if nextOpenSpawn == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(spawnLocs[nextOpenSpawn]);
            super.justSpawned = true;

            super.atSpawnActions();
        }
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

        return minDistID;
    }
}
