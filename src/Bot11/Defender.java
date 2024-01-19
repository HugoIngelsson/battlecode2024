package Bot11;

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
    MapLocation spawnCenter;
    int flagId;
    Random rng;
    boolean searching;


    public Defender(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.searching = true;
        this.flagId = id % 3;
        this.curDest = FastMath.getRandomMapLocation();;
        this.rng = new Random();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        if (rc.getRoundNum() > 10 && !justDied) {
            byteZero = rc.readSharedArray(0);
            //System.out.println(flagId + " just died! The bit is " + RobotPlayer.bitAt(byteZero, 7 - flagId) + " and the byte is " + Integer.toBinaryString(rc.readSharedArray(0)));
            justDied = true;
            //System.out.println("pre byte zero: " + byteZero);
            byteZero -= Math.pow(2, 7 - flagId);
            rc.writeSharedArray(0, byteZero);
            //System.out.println("post byte zero: " + byteZero);
            //System.out.println("Now the bit is " + RobotPlayer.bitAt(byteZero, 7 - flagId) + " and the byte is " + Integer.toBinaryString(rc.readSharedArray(0)));
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] subSpawns = new MapLocation[9];

        MapLocation findFlagLoc = MapHelper.poseDecoder(rc.readSharedArray(1 + flagId));
        spawnCenter = findFlagLoc;

        int idx = 0;
        for (int i = 0; i < spawnLocs.length; i++) {
            if (spawnLocs[i].distanceSquaredTo(findFlagLoc) < 4) { 
                subSpawns[idx] = spawnLocs[i];
                idx++;
            }
        }


        if (rc.getRoundNum() < 3) {
            System.out.println("flagId" + (flagId + 1));
            System.out.println(rc.readSharedArray(1 + flagId));
            System.out.println("spawn center:" + spawnCenter);
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
        byteZero = rc.readSharedArray(0);
        if (!RobotPlayer.bitAt(byteZero, 7 - (flagId))) {
            System.out.println(flagId + " just spawned! The bit is " + RobotPlayer.bitAt(byteZero, 7 - flagId) + " and the byte is " + Integer.toBinaryString(rc.readSharedArray(0)));
            turnOnSafetyByte(flagId);
            System.out.println("Now the bit is " + RobotPlayer.bitAt(byteZero, 7 - flagId) + " and the byte is " + Integer.toBinaryString(rc.readSharedArray(0)));
        }
        //FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);

        if (rc.getRoundNum() < 200 && rc.getExperience(SkillType.BUILD) < 30) {
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

            curDest = spawnCenter;
            if (rc.getLocation().distanceSquaredTo(spawnCenter) == 0) {
                if (rc.isMovementReady() && rc.canMove(spawnCenter.directionTo(rc.getLocation()))) {
                    rc.move(spawnCenter.directionTo(rc.getLocation()));
                }
            } 
            if (rc.getLocation().distanceSquaredTo(curDest) < 9) {
                curDest = rc.getLocation();
            }
        }
        else {
            dist = spawnCenter.distanceSquaredTo(rc.getLocation());

            if (dist <= DEFENSE_RADIUS && rc.canBuild(TrapType.STUN, rc.getLocation())) {
                if (dist <= BOMB_DEFENSE_RADIUS && (rc.getLocation().x+rc.getLocation().y)%2==0) {
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
                else rc.build(TrapType.STUN, rc.getLocation());
            }

            if (rc.isActionReady() && rc.getRoundNum() > 200) {
                if (super.getNearbyEnemies().length > 0) {
                    RobotInfo weakestAttackable = Micro.getBestTarget(super.getNearbyEnemies(), super.getNearbyAllies(), damage);

                    if (weakestAttackable != null) {
                        tempTarget = weakestAttackable.getLocation();
                        rc.attack(weakestAttackable.getLocation());
                    }
                }
            }

            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(spawnCenter, DEFENSE_RADIUS);
            if (nearbyTiles != null) {
                ArrayList<MapInfo> emptyTiles = new ArrayList<>();
                for (int i = 0; i < nearbyTiles.length; i++) {
                    if (spawnCenter.distanceSquaredTo(nearbyTiles[i].getMapLocation()) <= DEFENSE_RADIUS &&
                            nearbyTiles[i].getTrapType() == TrapType.NONE) {
                        emptyTiles.add(nearbyTiles[i]);
                    }
                }
                if (!emptyTiles.isEmpty()) {
                    curDest = emptyTiles.get(rng.nextInt(emptyTiles.size())).getMapLocation();
                } else {
                    curDest = spawnCenter;
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