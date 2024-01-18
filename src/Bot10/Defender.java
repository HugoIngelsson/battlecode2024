package Bot10;

import Bot10.fast.FastMath;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Defender extends Robot {
    static final int DEFENSE_RADIUS = 2;
    static final int BOMB_DEFENSE_RADIUS = 2;

    RobotController rc;
    int id;
    int dist;
    FlagInfo foundFlag;
    int flagId;
    Random rng;
    boolean searching;

    public Defender(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.foundFlag = null;
        this.searching = true;
        this.flagId = id % 3;
        this.curDest = FastMath.getRandomMapLocation();;
        this.rng = new Random();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        if (lastTurn != 0 && !justDied) {
            justDied = true;
            byteZero -= Math.pow(2, 7 - (flagId % 3));
            rc.writeSharedArray(0, byteZero);
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] subSpawns = null;

        if (searching) {
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
        } else {
            subSpawns = Arrays.copyOfRange(spawnLocs, 9*(flagId % 3), 9+9*(flagId % 3));
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
        if (!searching && !RobotPlayer.bitAt(0, 7 - (flagId % 3))) {
            turnOnSafetyByte(flagId);
        }
        // debug
        /*if (rc.getRoundNum() == 100) {
            System.out.println("curDest:" + curDest);
            System.out.println("flagId:" + flagId);
            System.out.println("arrayFlagLoc:" + MapHelper.poseDecoder(rc.readSharedArray(flagId + 1)));
        }*/

        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);

        /*if (rc.getRoundNum() % 100 == 0) {
            System.out.println("nearby flags:" + nearbyFlags.length);
            System.out.println("loc:" + rc.getLocation());
        }*/
        if (rc.getRoundNum() == 3) {
            MapLocation nearestFlagLoc = nearbyFlags[0].getLocation();
            if (searching && nearestFlagLoc.equals(MapHelper.poseDecoder(rc.readSharedArray(flagId + 1))) && 
            RobotPlayer.bitAt(0, 7 - (flagId) % 3)) {
                turnOnSafetyByte(flagId);
                searching = false;
            }
            if (searching && nearestFlagLoc.equals(MapHelper.poseDecoder(rc.readSharedArray((flagId + 1) % 3 + 1))) &&
            RobotPlayer.bitAt(0, 7 - (flagId + 1) % 3)) {
                turnOnSafetyByte(flagId);
                searching = false;
                flagId = (flagId + 1) % 3;
            }
            if (searching && nearestFlagLoc.equals(MapHelper.poseDecoder(rc.readSharedArray((flagId + 2) % 3 + 1))) && 
            RobotPlayer.bitAt(0, 7 - (flagId + 2) % 3)) {
                turnOnSafetyByte(flagId);
                searching = false;
                flagId = (flagId + 2) % 3;
            }
            //System.out.print("curDest is:" + (flagId + 1));
            //System.out.println("which is at " + MapHelper.poseDecoder(rc.readSharedArray(flagId + 1)));
            curDest = MapHelper.poseDecoder(rc.readSharedArray(flagId + 1));
            //System.out.println(curDest);
        } else if (rc.getRoundNum() < 200 && rc.getExperience(SkillType.BUILD) < 30) {
            if (rc.isActionReady()) {
                for (MapInfo mi : rc.senseNearbyMapInfos(2)) {
                    if (mi.isWater() && rc.canFill(mi.getMapLocation())) {
                        rc.fill(mi.getMapLocation());
                        break;
                    }
                }
            }

            if (rc.isActionReady()) {
                for (Direction d : Direction.allDirections()) {
                    if (rc.canDig(rc.getLocation().add(d))) {
                        rc.dig(rc.getLocation().add(d));
                        break;
                    }
                }
            }

            curDest = MapHelper.poseDecoder(rc.readSharedArray(flagId + 1));
            if (rc.getLocation().distanceSquaredTo(curDest) < 9) {
                curDest = rc.getLocation();
            }
        }
        else {
            if (foundFlag == null && nearbyFlags.length > 0) {
                System.out.println("finding flag!");
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
                if (foundFlag == null) {
                    dist = Integer.MAX_VALUE;
                } else {
                    dist = foundFlag.getLocation().distanceSquaredTo(rc.getLocation());
                }
            }

            if (dist <= DEFENSE_RADIUS && rc.canBuild(TrapType.STUN, rc.getLocation())) {
                if (dist <= BOMB_DEFENSE_RADIUS && (rc.getLocation().x+rc.getLocation().y)%2==0) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
                else rc.build(TrapType.STUN, rc.getLocation());
            }

            if (rc.isActionReady() && rc.getRoundNum() > 200) {
                if (super.getNearbyEnemies().length > 0) {
                    RobotInfo weakestAttackable = null;

                    for (RobotInfo enemy : super.getNearbyEnemies()) {
                        if (rc.canAttack(enemy.getLocation())) {
                            if (weakestAttackable == null || weakestAttackable.getHealth() > enemy.getHealth()) {
                                weakestAttackable = enemy;
                            }
                        }

                        if (enemy.hasFlag()) { // if the enemy has a flag, we want to focus it
                            if (rc.canAttack(enemy.getLocation())) {
                                weakestAttackable = enemy;
                                break;
                            }

                            tempTarget = enemy.getLocation();
                        }
                    }

                    if (weakestAttackable != null) {
                        rc.attack(weakestAttackable.getLocation());
                    }
                }
            }

            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(foundFlag.getLocation(), DEFENSE_RADIUS);
            if (nearbyTiles != null) {
                ArrayList<MapInfo> emptyTiles = new ArrayList<>();
                for (int i = 0; i < nearbyTiles.length; i++) {
                    if (foundFlag.getLocation().distanceSquaredTo(nearbyTiles[i].getMapLocation()) <= DEFENSE_RADIUS &&
                            nearbyTiles[i].getTrapType() == TrapType.NONE) {
                        emptyTiles.add(nearbyTiles[i]);
                    }
                }
                if (!emptyTiles.isEmpty()) {
                    curDest = emptyTiles.get(rng.nextInt(emptyTiles.size())).getMapLocation();
                } else {
                    curDest = foundFlag.getLocation();
                }
            }
        }


        if (rc.isMovementReady()) {
            if (tempTarget != null) super.move(tempTarget);
            else super.move(curDest);
        }
    }

    void turnOnSafetyByte(int id) throws GameActionException {
        switch (flagId) {
            case 0:
                byteZero |= 0x0080;
                break;
            case 1:
                byteZero |= 0x0040;
                break;
            case 2:
                byteZero |= 0x0020;
                break;
            default:
                System.out.println("oops");
                break;
        }
        rc.writeSharedArray(0, byteZero);
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