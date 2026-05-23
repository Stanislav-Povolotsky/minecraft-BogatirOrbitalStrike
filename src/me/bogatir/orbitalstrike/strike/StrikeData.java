package me.bogatir.orbitalstrike.strike;

/**
 * Immutable-ish data holder for a single orbital strike configuration.
 * Implements Cloneable so the GUI can pass a snapshot to StrikeTask.
 */
public class StrikeData implements Cloneable {

    // ─── Target ────────────────────────────────────────────────────────────────
    private double x       = 0;
    private double y       = 64;
    private double z       = 0;
    private String world   = "world";

    // ─── Strike parameters ─────────────────────────────────────────────────────
    /** Ticks before the animation sequence begins. */
    private int delayTicks   = 100;
    /** Strike power 1–10. Controls explosion size + fire radius. */
    private int power        = 5;
    /** How many times to repeat the strike sequence at the same location. */
    private int count        = 1;

    // ─── Fear effect ───────────────────────────────────────────────────────────
    /** Intensity of the Fear effect (1–10). */
    private int fearPower    = 3;
    /** Duration of the Fear effect in ticks. */
    private int fearDuration = 100;

    // ─── Getters ───────────────────────────────────────────────────────────────

    public double getX()           { return x; }
    public double getY()           { return y; }
    public double getZ()           { return z; }
    public String getWorld()       { return world; }
    public int    getDelayTicks()  { return delayTicks; }
    public int    getPower()       { return power; }
    public int    getCount()       { return count; }
    public int    getFearPower()   { return fearPower; }
    public int    getFearDuration(){ return fearDuration; }

    // ─── Setters ───────────────────────────────────────────────────────────────

    public void setX(double x)                 { this.x = x; }
    public void setY(double y)                 { this.y = y; }
    public void setZ(double z)                 { this.z = z; }
    public void setWorld(String world)         { this.world = world; }
    public void setDelayTicks(int delayTicks)  { this.delayTicks = Math.max(1, delayTicks); }
    public void setPower(int power)            { this.power = Math.min(10, Math.max(1, power)); }
    public void setCount(int count)            { this.count = Math.max(1, count); }
    public void setFearPower(int fearPower)    { this.fearPower = Math.min(10, Math.max(0, fearPower)); }
    public void setFearDuration(int dur)       { this.fearDuration = Math.max(0, dur); }

    // ─── Clone ─────────────────────────────────────────────────────────────────

    @Override
    public StrikeData clone() {
        try {
            return (StrikeData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "StrikeData{world=%s, xyz=(%.1f,%.1f,%.1f), delay=%d, power=%d, count=%d, fearPower=%d, fearDur=%d}",
            world, x, y, z, delayTicks, power, count, fearPower, fearDuration
        );
    }
}
