package org.jbei.ice.shared.dto;

public class BlastResultInfo extends HasEntryInfo {

    private float bitScore;
    private float eValue;
    private int alignmentLength;
    private float percentId;
    private int queryLength;

    public int getAlignmentLength() {
        return alignmentLength;
    }

    public void setAlignmentLength(int alignmentLength) {
        this.alignmentLength = alignmentLength;
    }

    public float getPercentId() {
        return percentId;
    }

    public void setPercentId(float percentId) {
        this.percentId = percentId;
    }

    public float getBitScore() {
        return bitScore;
    }

    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }

    public float geteValue() {
        return eValue;
    }

    public void seteValue(float eValue) {
        this.eValue = eValue;
    }

    public int getQueryLength() {
        return queryLength;
    }

    public void setQueryLength(int queryLength) {
        this.queryLength = queryLength;
    }
}