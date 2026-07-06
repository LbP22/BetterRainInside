package com.betterraininside;

public final class Config {
    public boolean enabled = true;
    public float minimumVolume = 0.02f;
    public float maximumIndoorVolume = 0.1f;
    public int sampleRadius = 3;
    public int updateIntervalTicks = 5;
    public float transitionSpeed = 0.18f;

    // Detection tuning.
    public int wallProbeDistance = 5;
    public float roofCoverageWeight = 0.6f;
    public float wallCoverageWeight = 0.4f;
    public float enclosureCurve = 1.7f;
    public float treeCanopyRelief = 0.35f;
    public float detectionSensitivity = 1.65f;
    public float roofedSpaceBoost = 0.2f;

    // Volume mapping tuning.
    public float indoorResponseCurve = 1.8f;

    public void sanitize() {
        this.minimumVolume = clamp(this.minimumVolume, 0.0f, 1.0f);
        this.maximumIndoorVolume = clamp(this.maximumIndoorVolume, this.minimumVolume, 1.0f);
        this.sampleRadius = clamp(this.sampleRadius, 1, 8);
        this.updateIntervalTicks = clamp(this.updateIntervalTicks, 1, 40);
        this.transitionSpeed = clamp(this.transitionSpeed, 0.01f, 1.0f);
        this.wallProbeDistance = clamp(this.wallProbeDistance, 2, 12);
        this.roofCoverageWeight = clamp(this.roofCoverageWeight, 0.0f, 1.0f);
        this.wallCoverageWeight = clamp(this.wallCoverageWeight, 0.0f, 1.0f);
        float sum = this.roofCoverageWeight + this.wallCoverageWeight;
        if (sum <= 0.0001f) {
            this.roofCoverageWeight = 0.6f;
            this.wallCoverageWeight = 0.4f;
        } else {
            this.roofCoverageWeight /= sum;
            this.wallCoverageWeight /= sum;
        }
        this.enclosureCurve = clamp(this.enclosureCurve, 0.5f, 3.0f);
        this.treeCanopyRelief = clamp(this.treeCanopyRelief, 0.0f, 0.8f);
        this.detectionSensitivity = clamp(this.detectionSensitivity, 0.5f, 3.0f);
        this.roofedSpaceBoost = clamp(this.roofedSpaceBoost, 0.0f, 0.5f);
        this.indoorResponseCurve = clamp(this.indoorResponseCurve, 0.5f, 3.0f);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
