package Bot5;

import battlecode.common.*;

public class Attacker extends Robot {
    RobotController rc;
    int id;

    public Attacker(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.curDest = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);

//        this.curDest = FastMath.getRandomMapLocation();
    }

    void play() throws GameActionException {
        if (rc.hasFlag()) {
            // do something
            curDest = MapHelper.poseDecoder(rc.readSharedArray(1));

            PathFinding.move(curDest);
            return;
        }
        else if (super.getEnemyFlags().length > 0) {
            if (!super.getEnemyFlags()[0].isPickedUp() || super.getEnemyFlags()[0].getLocation().distanceSquaredTo(rc.getLocation()) > 4)
                tempTarget = super.getEnemyFlags()[0].getLocation();

            if (rc.getLocation().equals(tempTarget))
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

                // if possible, run away from the enemy to kite it/avoid unnecessary attacks
                if (rc.isMovementReady() && rc.canMove(weakestAttackable.getLocation().directionTo(rc.getLocation())))
                    rc.move(weakestAttackable.getLocation().directionTo(rc.getLocation()));
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

        if (super.getNearbyCrumbs().length > 0) {
            tempTarget = super.getNearbyCrumbs()[0];
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
}
