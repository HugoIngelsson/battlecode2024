package Bot20;

import battlecode.common.*;

class Micro {
    // inspired by Gone Fishin' 2023 bot :)
    public static final int MAX_HEALTH = 1000;
    public static final int DAMAGE = 150;
    public static final int HEAL = 80;
    public static final int ATTACK_DIS = 4;
    public static final int VISION_DIS = 20;

    static Team myTeam;
    static Team enemyTeam;

    static RobotInfo attackTarget = null;
    static RobotInfo chaseTarget = null;
    static MapLocation cachedEnemyLocation = null;
    static RobotInfo healTarget = null;
    static RobotInfo groupingTarget = null;

    private static final int MAX_ENEMY_CNT = 12;
    static RobotInfo[] enemies = new RobotInfo[MAX_ENEMY_CNT];
    static int enemyCount;
    static int enemyStrength;
    private static final int MAX_FRIENDLY_CNT = 10;
    static RobotInfo[] allies = new RobotInfo[MAX_FRIENDLY_CNT];
    static int allyCount;
    static int allyStrength;
    static int closeFriendsSize;
    static int lastAttackRound = -100;

    static int ENEMY_ATTACK_BONUS = 0;
    static int ALLY_ATTACK_BONUS = 0;
    static int ALLY_HEAL_BONUS = 0;

    static RobotController rc;
    public static void init(RobotController r){
        rc = r;
        myTeam = r.getTeam();
        enemyTeam = r.getTeam().opponent();
    }

    static void sense() throws GameActionException {
        if (ENEMY_ATTACK_BONUS == 0) {
            if (containsAttackUpgrade(rc.getGlobalUpgrades(enemyTeam)))
                ENEMY_ATTACK_BONUS = 60;
            if (rc.getRoundNum() == 601) // we buy the upgrade on round 601
                ALLY_ATTACK_BONUS = 60;
        }
        if (rc.getRoundNum() == 1201) // we buy the upgrade on round 1201
            ALLY_HEAL_BONUS = 50;

        attackTarget = null;
        chaseTarget = null;
        healTarget = null;
        groupingTarget = null;
        allyCount = 0;
        allyStrength = 0;
        enemyCount = 0;
        enemyStrength = 0;
        closeFriendsSize = 0;

        int leastHealthAlly = Integer.MAX_VALUE;
        int leastHealthAllyDist = Integer.MAX_VALUE;
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.team == myTeam) {
                if (allyCount >= MAX_FRIENDLY_CNT)
                    continue;

                // find a healthy ally to follow
                if (groupingTarget == null ||
                    groupingTarget.health < robot.health) {
                    groupingTarget = robot;
                }

                allies[allyCount++] = robot;
                allyStrength += expectedAllyDamageOutput(robot);
                if (robot.location.distanceSquaredTo(rc.getLocation()) <= 8) {
                    closeFriendsSize++;

                    // sense a suitable heal target
                    if (robot.location.distanceSquaredTo(rc.getLocation()) <= 4) {
                        if (robot.health < leastHealthAlly) {
                            leastHealthAllyDist = robot.location.distanceSquaredTo(rc.getLocation());
                            leastHealthAlly = robot.health;
                            healTarget = robot;
                        }
                        else if (robot.health == leastHealthAlly &&
                                robot.location.distanceSquaredTo(rc.getLocation()) < leastHealthAllyDist) {
                            leastHealthAllyDist = robot.location.distanceSquaredTo(rc.getLocation());
                            healTarget = robot;
                        }
                    }
                }
            }
            else {
                if (enemyCount >= MAX_ENEMY_CNT)
                    continue;

                enemies[enemyCount++] = robot;
                enemyStrength += expectedEnemyDamageOutput(robot);
                if (robot.location.distanceSquaredTo(rc.getLocation()) > ATTACK_DIS)
                    chaseTarget = robot;
            }
        }

        attackTarget = getBestTarget();
    }

    static void healMicro() throws GameActionException {
        if (healTarget != null && rc.canHeal(healTarget.location)) {
            int healingOutput = rc.getHealAmount();
            for (int i = allyCount; --i >= 0;) {
                if (allies[i].location.distanceSquaredTo(healTarget.location) <= 4) {
                    healingOutput += expectedAllyHeal(allies[i]);
                }
            }

            if (healTarget.health + healingOutput - enemyStrength > 0)
                rc.heal(healTarget.location);
        }
    }

    static void buildMicro() throws GameActionException {
        int TRAP_DISTANCE = 4;
        if (rc.getCrumbs() > 1000) TRAP_DISTANCE -= 2;

        if (rc.canBuild(TrapType.STUN, rc.getLocation()) && enemyCount > 2) {
            int closestTrap = Integer.MAX_VALUE;
            for (MapInfo info : rc.senseNearbyMapInfos(6)) {
                if (info.getTrapType() != TrapType.NONE)
                    closestTrap = Math.min(closestTrap, info.getMapLocation().distanceSquaredTo(rc.getLocation()));
            }

            if (closestTrap > TRAP_DISTANCE) {
                if (rc.getCrumbs() > 5000)
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                else
                    rc.build(TrapType.STUN, rc.getLocation());
            }
        }
    }

    static void attackMicro() throws GameActionException {
        RobotInfo target = attackTarget;
        if (target != null) {
            lastAttackRound = rc.getRoundNum();

            RobotInfo deadTarget = null;
            if (rc.canAttack(target.location)) {
                if (target.health <= rc.getAttackDamage()) {
                    deadTarget = target;
                }
                rc.attack(target.location);
            }

            int minDist = Integer.MAX_VALUE;
            cachedEnemyLocation = null;
            for (int i = enemyCount; --i >= 0;) {
                RobotInfo enemy = enemies[i];
                int dist = enemy.location.distanceSquaredTo(rc.getLocation());
                if (enemy != deadTarget && dist < minDist) {
                    cachedEnemyLocation = enemy.location;
                    minDist = dist;
                }
            }

            if (cachedEnemyLocation != null && rc.isMovementReady()) {
                kite(cachedEnemyLocation);
            }
        }

        if (rc.isMovementReady() && rc.isActionReady()) {
            if (chaseTarget != null) {
                cachedEnemyLocation = chaseTarget.location;

                if (allyStrength - enemyStrength > 100) {
                    chase(chaseTarget.location);
                } else {
                    kite(chaseTarget.location);
                }
            }
        }
    }

    static void chase(MapLocation location) throws GameActionException {
        Direction forwardDir = rc.getLocation().directionTo(location);
        Direction[] dirs = {forwardDir, forwardDir.rotateLeft(), forwardDir.rotateRight(),
                forwardDir.rotateRight().rotateRight(), forwardDir.rotateLeft().rotateLeft()};
        Direction bestDir = null;
        int minClose = Integer.MAX_VALUE;
        for (Direction dir : dirs) {
            if (rc.canMove(dir) && rc.getLocation().add(dir).distanceSquaredTo(location) <= ATTACK_DIS) {
                int danger = 0;
                for (int i = enemyCount; --i >= 0;) {
                    int dist = rc.getLocation().add(dir).distanceSquaredTo(enemies[i].location);
                    if (dist <= ATTACK_DIS)
                        danger++;
                }
                if (danger < minClose) {
                    minClose = danger;
                    bestDir = dir;
                }
            }
        }

        if (bestDir != null) {
            rc.move(bestDir);
        }
    }

    static void kite(MapLocation location) throws GameActionException {
        Direction backDir = rc.getLocation().directionTo(location);
        Direction[] dirs = {Direction.CENTER, backDir, backDir.rotateLeft(), backDir.rotateRight(),
                backDir.rotateRight().rotateRight(), backDir.rotateLeft().rotateLeft()};
        Direction bestDir = null;
        int minClose = Integer.MAX_VALUE;
        for (Direction dir : dirs) {
            if (rc.canMove(dir) && rc.getLocation().add(dir).distanceSquaredTo(location) <= ATTACK_DIS) {
                int danger = 0;
                for (int i = enemyCount; --i >= 0;) {
                    int dist = rc.getLocation().add(dir).distanceSquaredTo(enemies[i].location);
                    if (dist <= ATTACK_DIS)
                        danger++;
                }
                if (danger < minClose) {
                    minClose = danger;
                    bestDir = dir;
                }
            }
        }

        if (bestDir != null && bestDir != Direction.CENTER) {
            rc.move(bestDir);
        }
    }

    static RobotInfo getBestTarget(){
        int minHitRequired = Integer.MAX_VALUE;
        RobotInfo rv = null;
        int minDis = Integer.MAX_VALUE;
        for (int i = enemyCount; --i >= 0;) {
            RobotInfo enemy = enemies[i];
            int dist = rc.getLocation().distanceSquaredTo(enemy.location);

            if (dist > ATTACK_DIS)
                continue;

            int friendAttackPower = rc.getAttackDamage();
            if (enemy.getHealth() <= friendAttackPower)
                return enemy;

            for (int j = allyCount; --j >= 0;) {
                int dst = allies[j].location.distanceSquaredTo(enemy.location);
                if (dst <= ATTACK_DIS)
                    friendAttackPower += expectedAllyDamageOutput(allies[j]);
            }

            int hitRequired = enemy.getHealth() / friendAttackPower + 1;
            if (hitRequired < minHitRequired) {
                minHitRequired = hitRequired;
                rv = enemy;
                minDis = enemy.location.distanceSquaredTo(rc.getLocation());
            }
            else if (hitRequired == minHitRequired) {
                if (dist < minDis) {
                    rv = enemy;
                    minDis = dist;
                }
            }
        }

        return rv;
    }

    static int expectedAllyDamageOutput(RobotInfo robot) {
        int attackLevel = robot.getAttackLevel();
        return (int)((DAMAGE + ALLY_ATTACK_BONUS) *
                            Constants.attackBuff[attackLevel] *
                        (1 / Constants.attackCooldown[attackLevel])
               );
    }

    static int expectedEnemyDamageOutput(RobotInfo robot) {
        int attackLevel = robot.getAttackLevel();
        return (int)((DAMAGE + ENEMY_ATTACK_BONUS) *
                Constants.attackBuff[attackLevel] *
                (0.5 / Constants.attackCooldown[attackLevel])
        );
    }

    static int expectedAllyHeal(RobotInfo robot) {
        int healLevel = robot.getHealLevel();
        return (int)((HEAL + ALLY_HEAL_BONUS) *
                Constants.healBuff[healLevel] *
                (0.333 / Constants.healCooldown[healLevel])
        );
    }

    static boolean containsAttackUpgrade(GlobalUpgrade[] upgrades) {
        for (GlobalUpgrade u : upgrades) {
            if (u.equals(GlobalUpgrade.ATTACK)) return true;
        }

        return false;
    }
}
