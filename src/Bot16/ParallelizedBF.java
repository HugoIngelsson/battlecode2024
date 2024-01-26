package Bot16;

import battlecode.common.*;

import java.util.HashSet;

public class ParallelizedBF {
    static final int WATER_COST = 6;
    static final Direction[] directions = {
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.WEST,
            Direction.CENTER,
            Direction.EAST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.NORTHEAST
    };

    static HashSet<Integer> visited = new HashSet<>();

    static long waterFilter;
    static long walkableFilter;
    static long[] iterations;

    RobotController rc;
    MapLocation lastTarget;

    public ParallelizedBF(RobotController rc) {
        this.rc = rc;
        this.lastTarget = null;
    }

    public Direction BF(MapLocation target) throws GameActionException {
        if (!target.equals(lastTarget)) {
            visited.clear();
        }

        visited.add(getCode(rc.getLocation()));
        lastTarget = target;

        iterations = new long[20];
        waterFilter = 0;
        walkableFilter = 0;

        MapLocation robotLocation = rc.getLocation();
        int greedyRobotDistance = greedyDistance(robotLocation, target);
        int deltaX = robotLocation.x - 3;
        int deltaY = robotLocation.y - 3;

        if (greedyRobotDistance < 3) {
            int shift = (target.x - deltaX) + 8 * (target.y - deltaY);
            iterations[0] |= 1L<<shift;

            if (greedyRobotDistance <= 1) return robotLocation.directionTo(target);
        }
        for (MapInfo info : rc.senseNearbyMapInfos(-1)) {
            MapLocation location = info.getMapLocation();
            int shift = (location.x - deltaX) + 8 * (location.y - deltaY);

            int greedyDist = greedyDistance(location, robotLocation);
            if (greedyDist <= 3) {
                if (greedyRobotDistance > 3 &&
                        greedyDist == 3 && strictlyCloser(robotLocation, location, target)) {
                    int id = greedyDistance(location, target) - greedyRobotDistance + 4;

                    if (info.isPassable()) {
                        iterations[id] |= 1L<<shift;
                    }
                    else if (info.isWater()) {
                        iterations[id+WATER_COST-1] |= 1L<<shift;
                    }
                }

                if (info.isPassable() && !info.isDam()) {
                    walkableFilter |= 1L<<shift;
                }
                else if (info.isWater()) {
                    waterFilter |= 1L<<shift;
                }
            }
        }

        if (rc.hasFlag() || !rc.isActionReady()) waterFilter = 0;

        for (int i=0; i<iterations.length-WATER_COST; i++) {
            long waters = iterations[i] & waterFilter;
            long lands = iterations[i] & walkableFilter;

            long nextWaterHoriz = waters | (waters<<1) | (waters>>1);
            long nextWater = nextWaterHoriz | (nextWaterHoriz<<8) | (nextWaterHoriz>>8);

            long nextLandHoriz = lands | (lands<<1) | (lands>>1);
            long nextLand = nextLandHoriz | (nextLandHoriz<<8) | (nextLandHoriz>>8);

            iterations[i+1] |= nextLand;
            iterations[i+WATER_COST] |= nextWater;
        }

        long directionFinder = 0;
        for (int i=0; i<iterations.length; i++) {
            if ((iterations[i] & 0x8000000) > 0) {
                directionFinder = iterations[i-1] & (walkableFilter | waterFilter);
                break;
            }
        }

        Direction minDir = null;
        int minDist = Integer.MAX_VALUE;
        for (int i=2; i<=4; i++) {
            for (int j=2; j<=4; j++) {
                if ((directionFinder & (1L<<(j+i*8))) > 0) {
                    Direction d = directions[3*(i-2)+(j-2)];
                    MapLocation dest = rc.getLocation().add(d);
                    if ((rc.canMove(d) || rc.senseMapInfo(dest).isWater()) &&
                            rc.getLocation().distanceSquaredTo(dest) < minDist &&
                            !visited.contains(getCode(dest))) {
                        minDir = d;
                        minDist = rc.getLocation().distanceSquaredTo(dest);
                    }
                }
            }
        }

        return minDir;
    }

    public static void bitBoardVizualizer(long bitboard) {
        for (int i=7; i>=0; i--) {
            String print = "";
            for (int j=0; j<8; j++) {
                if ((bitboard&(1L<<(j+i*8))) > 0) {
                    print += "#";
                }
                else print += ".";
            }

            System.out.println(print);
        }
    }

    public static int greedyDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    public static boolean strictlyCloser(MapLocation a, MapLocation b, MapLocation target) {
        return a.distanceSquaredTo(target) > b.distanceSquaredTo(target);
    }

    int getCode(MapLocation ml) {
        int x = ml.x;
        int y = ml.y;
        return x*100+y;
    }
}
