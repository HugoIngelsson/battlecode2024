package Bot2;

import Bot2.fast.FastMath;
import battlecode.common.*;
import java.util.Random;
import java.util.ArrayList;

public class Builder extends Robot {
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
        this.rng = new Random(727);
    }

    void play() throws GameActionException {
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
        if (rc.hasFlag()) {
            // do something
        }
        else if (super.getNearbyEnemies().length > 0 && rc.isActionReady()) { // see if we can attack an enemy
            if (super.getNearbyEnemies().length > 2 && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
            else {
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
                        System.out.println("atempted to move");
                        //rc.move(weakestAttackable.getLocation().directionTo(rc.getLocation()));
                }
            }

        }
        else if (super.getNearbyAllies().length > 0 && rc.isActionReady()) {
            RobotInfo weakestHealable = null;

            for (RobotInfo ally : super.getNearbyAllies()) {
                if (rc.canAttack(ally.getLocation())) {
                    if (weakestHealable == null || weakestHealable.getHealth() > ally.getHealth()) {
                        weakestHealable = ally;
                    }
                }

                if (ally.hasFlag()) { // if the ally has a flag, we want to defend it
                    // curDest = ally.getLocation();

                    if (rc.canAttack(ally.getLocation())) {
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
            // curDest = super.getNearbyCrumbs()[0];
        }


        if (foundFlag == null && nearbyFlags.length > 0) {
            int min = Integer.MAX_VALUE;
            int idx = -1;
            dist = Integer.MAX_VALUE;
            for (int i = 0; i < nearbyFlags.length; i++)  {
                dist = nearbyFlags[i].getLocation().distanceSquaredTo(rc.getLocation());
                if (dist == 1) {
                    idx = i;
                    break;
                }
                if (dist < 17) {
                    idx = i;
                    min = dist;
                }
            }
            foundFlag = nearbyFlags[idx];
            curDest = foundFlag.getLocation();
        } else {
            dist = foundFlag.getLocation().distanceSquaredTo(rc.getLocation());
        }


        if (dist <= 20 && rc.canBuild(TrapType.STUN, rc.getLocation())) {
            rc.build(TrapType.STUN, rc.getLocation());
            System.out.println("trap built, dist: " + dist);
            System.out.println("location: " + rc.getLocation());
            System.out.println("for flag at: " + foundFlag.getLocation());
        }
        
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(20);
        if (nearbyTiles != null) {
            ArrayList<MapInfo> emptyTiles = new ArrayList<>();
            for (int i = 0; i < nearbyTiles.length; i++) {
                if (foundFlag.getLocation().distanceSquaredTo(nearbyTiles[i].getMapLocation()) <= 20 && nearbyTiles[i].getTrapType() == TrapType.NONE) {
                    emptyTiles.add(nearbyTiles[i]);
                }
            }
            if (emptyTiles.size() > 0) {
                curDest = emptyTiles.get(rng.nextInt(emptyTiles.size())).getMapLocation();
            } else {
                curDest = foundFlag.getLocation();
            }
        }
        

        if (rc.isMovementReady()) {
            Direction next = super.pathfind(curDest);
            

            if (next != null) rc.move(next);
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