package Bot11;

import Bot10.fast.FastMath;
import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

public class Builder extends Robot {
    static final int DEFENSE_RADIUS = 2;
    static final int BOMB_DEFENSE_RADIUS = 2;

    RobotController rc;
    int id;
    int dist;
    FlagInfo foundFlag;
    Random rng;

    public Builder(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.foundFlag = null;
        this.curDest = FastMath.getRandomMapLocation();;
        this.rng = new Random();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] subSpawns = null;

        switch (id % 3) {
            case 0:
                subSpawns = Arrays.copyOfRange(spawnLocs, 0, 9);
                break;
            case 1:
                subSpawns = Arrays.copyOfRange(spawnLocs, 9, 18);
                break;
            case 2:
                subSpawns = Arrays.copyOfRange(spawnLocs, 18, 27);
                break;
            default:
                break;
        }

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(subSpawns, id%9);

        // if id == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(subSpawns[nextOpenSpawn]);
            super.justSpawned = true;

            super.atSpawnActions();
        }
    }

    void play() throws GameActionException {
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
        if (rc.hasFlag()) {
            curDest = super.getClosestBase();

            super.passFlag();

            super.move(curDest);

            return;
        }
        else if (super.getEnemyFlags().length > 0) {
            FlagInfo flag = super.getEnemyFlags()[0];
            if (!flag.isPickedUp())
                tempTarget = flag.getLocation();
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) <= 2) {
                if (rc.canMove(flag.getLocation().directionTo(rc.getLocation()))) {
                    rc.move(flag.getLocation().directionTo(rc.getLocation()));
                }
            }
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) > 4)
                tempTarget = flag.getLocation();

            if (rc.getLocation().equals(tempTarget))
                if (!rc.canPickupFlag(tempTarget)) tempTarget = null;
                else if (rc.isActionReady()) {
                    rc.pickupFlag(tempTarget);
                    hadFlag = true;
                    lastTurn = rc.getRoundNum();
                }
        }
        else if (super.getNearbyEnemies().length > 0 && rc.isActionReady()) { // see if we can attack an enemy
            if (super.getNearbyEnemies().length > 2 && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
            else {
                RobotInfo weakestAttackable = Micro.getBestTarget(super.nearbyEnemies, super.nearbyAllies, damage);

//                for (RobotInfo enemy : super.getNearbyEnemies()) {
//                    if (rc.canAttack(enemy.getLocation())) {
//                        if (weakestAttackable == null || weakestAttackable.getHealth() > enemy.getHealth()) {
//                            weakestAttackable = enemy;
//                        }
//                    }
//
//                    if (enemy.hasFlag()) { // if the enemy has a flag, we want to focus it
//                        if (rc.canAttack(enemy.getLocation())) {
//                            weakestAttackable = enemy;
//                            break;
//                        }
//                    }
//                }

                if (weakestAttackable != null) {
                    tempTarget = weakestAttackable.getLocation();
                    rc.attack(weakestAttackable.getLocation());

                    // if possible, run away from the enemy to kite it/avoid unnecessary attacks
                    if (rc.isMovementReady() && rc.canMove(weakestAttackable.getLocation().directionTo(rc.getLocation())))
                        rc.move(weakestAttackable.getLocation().directionTo(rc.getLocation()));
                }
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

        if (super.getNearbyCrumbs().length > 0) {
             curDest = super.getNearbyCrumbs()[0];
        }

        if (rc.isMovementReady()) {
            if (super.getNearbyAllies().length * 1.2 < super.getNearbyEnemies().length) {
                super.move(MapHelper.poseDecoder(rc.readSharedArray(7+id%3)));
            }
            else if (rc.hasFlag() || super.getNearbyEnemies().length <= FEAR_LIMIT) {
                if (tempTarget != null) super.move(tempTarget);
                else super.move(curDest);
            }
            else {
                super.move(super.allyCenter());
            }
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
}