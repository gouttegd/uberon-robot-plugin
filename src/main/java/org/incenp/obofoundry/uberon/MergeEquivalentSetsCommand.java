/*
 * Uberon ROBOT plugin
 * Copyright © 2023,2024 Damien Goutte-Gattat
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of copyright holder nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.incenp.obofoundry.uberon;

import org.apache.commons.cli.CommandLine;
import org.incenp.obofoundry.uberon.util.EquivalenceSetMerger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * A command to merge sets of equivalent classes across ontologies.
 * <p>
 * This command is based on OWLTools’ <code>--merge-equivalence-sets</code>
 * command.
 */
public class MergeEquivalentSetsCommand extends BasePlugin {

    public MergeEquivalentSetsCommand() {
        super("merge-equivalent-sets", "merge sets of equivalent classes",
                "robot merge-equivalent-sets [-s PREFIX[=SCORE]] [-l PREFIX[=SCORE]] [-c PREFIX[=SCORE]] [-d PREFIX[=SCORE]] [-P PREFIX]");

        options.addOption("s", "iri-priority", true, "order of priority to determine the representative IRI");
        options.addOption("l", "label-priority", true,
                "order of priority to determine which LABEL should be used post-merge");
        options.addOption("c", "comment-priority", true,
                "order of priority to determine which COMMENT should be used post-merge");
        options.addOption("d", "definition-priority", true,
                "order of priority to determine which DEFINITION should be used post-merge");
        options.addOption("p", "preserve", true, "disallow merging classes with the specified prefixes");

        options.addOption("r", "reasoner", true, "reasoner to use");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        EquivalenceSetMerger merger = new EquivalenceSetMerger();
        OWLDataFactory factory = state.getOntology().getOWLOntologyManager().getOWLDataFactory();

        if ( line.hasOption("s") ) {
            setScores(null, line.getOptionValues('s'), merger);
        }

        if ( line.hasOption("l") ) {
            setScores(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), line.getOptionValues('l'),
                    merger);
        }

        if ( line.hasOption("c") ) {
            setScores(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()),
                    line.getOptionValues('c'), merger);
        }

        if ( line.hasOption("d") ) {
            setScores(factory.getOWLAnnotationProperty(Obo2OWLConstants.Obo2OWLVocabulary.IRI_IAO_0000115.getIRI()),
                    line.getOptionValues('d'), merger);
        }

        if ( line.hasOption('p') ) {
            for ( String prefix : line.getOptionValues('p') ) {
                merger.addPreservedPrefix(prefix);
            }
        }

        merger.merge(state.getOntology(),
                CommandLineHelper.getReasonerFactory(line).createReasoner(state.getOntology()));
    }

    private void setScores(OWLAnnotationProperty p, String[] prefixes, EquivalenceSetMerger merger) {
        int autoScore = prefixes.length;
        for ( String prefix : prefixes ) {
            Double score = new Double(autoScore--);
            String[] parts = prefix.split("=", 2);
            if ( parts.length == 2 ) {
                prefix = parts[0];
                try {
                    score = Double.parseDouble(parts[1]);
                } catch ( NumberFormatException e ) {
                    throw new RuntimeException(
                            String.format("Invalid score value for prefix %s: %s\n", prefix, parts[1]));
                }
            }

            if ( p != null ) {
                merger.setPropertyPrefixScore(p, prefix, score);
            } else {
                merger.setPrefixScore(prefix, score);
            }
        }
    }
}
