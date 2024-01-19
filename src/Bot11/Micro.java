package Bot11;

import battlecode.common.*;

class Micro {
    static RobotController rc;
    static int attackDist = GameConstants.ATTACK_RADIUS_SQUARED;
    public static void init(RobotController r){
        rc = r;
    }
    static RobotInfo getBestTarget(RobotInfo[] enemies, RobotInfo[] allies, double damage){
        int minHitRequired = Integer.MAX_VALUE;
        RobotInfo rv = null;
        int minDis = Integer.MAX_VALUE;
        for(RobotInfo enemy: enemies){
            int dist = rc.getLocation().distanceSquaredTo(enemy.location);
            if(dist > GameConstants.ATTACK_RADIUS_SQUARED){
                continue;
            }
            if(damage>enemy.health){
                return enemy;
            }
            if (enemy.hasFlag()){
                return enemy;
            }
            double allyDamage = 0;
            for(RobotInfo ally:allies){
                int dst = ally.getLocation().distanceSquaredTo(enemy.location);
                if(dst <= attackDist){
                    allyDamage+=Util.damagePossible(ally);
                }
            }
            int hitsReq = (int) ((enemy.health) / (allyDamage + damage));
            if(hitsReq < minHitRequired){
                rv = enemy;
                minDis = dist;
                minHitRequired = hitsReq;
            } else if (hitsReq == minHitRequired) {
                if(dist < minDis){
                    rv = enemy;
                    minDis = dist;
                }
            }

        }
        return rv;
    }
}
