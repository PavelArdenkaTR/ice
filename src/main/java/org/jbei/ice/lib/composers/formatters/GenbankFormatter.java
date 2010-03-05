package org.jbei.ice.lib.composers.formatters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.ontology.InvalidTermException;
import org.biojava.utils.ChangeVetoException;
import org.biojavax.RichAnnotation;
import org.biojavax.RichObjectFactory;
import org.biojavax.SimpleNote;
import org.biojavax.SimpleRichAnnotation;
import org.biojavax.bio.seq.Position;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.SimplePosition;
import org.biojavax.bio.seq.SimpleRichFeature;
import org.biojavax.bio.seq.SimpleRichLocation;
import org.biojavax.bio.seq.SimpleRichSequence;
import org.biojavax.bio.seq.RichLocation.Strand;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.SequenceFeature;

public class GenbankFormatter extends AbstractFormatter {
    private String name;
    private String accessionNumber;
    private int version = 1;
    private double seqVersion = 1.0;
    private boolean circular = false;
    private String description = "";
    private String division = "";
    private String identifier = "";

    public GenbankFormatter(String name) {
        this(name, name, 1, 1.0);
    }

    public GenbankFormatter(String name, String accessionNumber, int version, double seqVersion) {
        super();

        this.name = name;
        this.accessionNumber = accessionNumber;
        this.version = version;
        this.seqVersion = seqVersion;
    }

    public boolean getCircular() {
        return circular;
    }

    public void setCircular(boolean value) {
        circular = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void format(Sequence sequence, OutputStream outputStream) throws FormatterException,
            IOException {
        if (sequence == null || outputStream == null || sequence.getSequence().isEmpty()) {
            return;
        }

        SimpleRichSequence simpleRichSequence = null;
        try {
            simpleRichSequence = new SimpleRichSequence(getNamespace(), name, accessionNumber,
                    version, DNATools.createDNA(sequence.getSequence()), seqVersion);

            simpleRichSequence.setCircular(getCircular());
            if (getDescription() != null && !getDescription().isEmpty()) {
                simpleRichSequence.setDescription(getDescription());
            }

            if (getDivision() != null && !getDivision().isEmpty()) {
                simpleRichSequence.setDivision(getDivision());
            }

            if (getIdentifier() != null && !getIdentifier().isEmpty()) {
                simpleRichSequence.setIdentifier(getIdentifier());
            }

            if (sequence.getSequenceFeatures() != null && sequence.getSequenceFeatures().size() > 0) {
                Set<Feature> featureSet = new LinkedHashSet<Feature>();

                for (SequenceFeature sequenceFeature : sequence.getSequenceFeatures()) {
                    if (sequenceFeature.getFeature() == null) {
                        Logger.warn("In sequence with id: " + sequence.getId()
                                + "; SequenceFeature object has no feature assigned to it.");

                        continue;
                    }

                    RichFeature.Template featureTemplate = new RichFeature.Template();
                    RichAnnotation richAnnotation = new SimpleRichAnnotation();
                    richAnnotation.addNote(new SimpleNote(RichObjectFactory.getDefaultOntology()
                            .getOrCreateTerm("label"), sequenceFeature.getName(), 1));

                    featureTemplate.annotation = richAnnotation;
                    featureTemplate.location = new SimpleRichLocation(
                            getMinPosition(sequenceFeature), getMaxPosition(sequenceFeature), 1,
                            getStrand(sequenceFeature));

                    featureTemplate.source = getDefaultFeatureSource();
                    featureTemplate.type = getFeatureType(sequenceFeature);
                    featureTemplate.rankedCrossRefs = new TreeSet<Object>();

                    featureSet.add(new SimpleRichFeature(simpleRichSequence, featureTemplate));
                }

                simpleRichSequence.setFeatureSet(featureSet);
            }
        } catch (IllegalSymbolException e) {
            throw new FormatterException("Failed to create generate genbank file", e);
        } catch (ChangeVetoException e) {
            throw new FormatterException("Failed to create generate genbank file", e);
        } catch (InvalidTermException e) {
            throw new FormatterException("Failed to create generate genbank file", e);
        }

        RichSequence.IOTools.writeGenbank(outputStream, simpleRichSequence, getNamespace());
    }

    protected Strand getStrand(SequenceFeature sequenceFeature) {
        Strand strand;

        if (sequenceFeature.getStrand() == -1) {
            strand = Strand.NEGATIVE_STRAND;
        } else if (sequenceFeature.getStrand() == 1) {
            strand = Strand.POSITIVE_STRAND;
        } else {
            strand = Strand.UNKNOWN_STRAND;
        }

        return strand;
    }

    protected Position getMinPosition(SequenceFeature sequenceFeature) {
        int min = sequenceFeature.getStart() < sequenceFeature.getEnd() ? sequenceFeature
                .getStart() : sequenceFeature.getEnd();

        return new SimplePosition(min);
    }

    protected Position getMaxPosition(SequenceFeature sequenceFeature) {
        int max = sequenceFeature.getStart() < sequenceFeature.getEnd() ? sequenceFeature.getEnd()
                : sequenceFeature.getStart();

        return new SimplePosition(max);
    }

    protected String getFeatureType(SequenceFeature sequenceFeature) {
        String featureType;

        if (sequenceFeature.getFeature().getGenbankType() == null
                || sequenceFeature.getFeature().getGenbankType().isEmpty()) {
            Logger.warn("SequenceFeature by id: " + sequenceFeature.getId()
                    + " has invalid genbank type.");

            featureType = "misc_feature";
        } else {
            featureType = sequenceFeature.getFeature().getGenbankType();
        }

        return featureType;
    }

    protected String getDefaultFeatureSource() {
        return "org.jbei";
    }
}