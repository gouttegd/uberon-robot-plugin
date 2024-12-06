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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.incenp.obofoundry.uberon.util.SpeciesMerger;
import org.obolibrary.robot.CommandLineHelper;
import org.obolibrary.robot.CommandState;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * A command to fold species-specific classes to form a “composite” ontology.
 * <p>
 * This command is the critical component of Uberon’s “composite-metazoan”
 * pipeline. It replaces OWLTools’s <code>--merge-species-ontology</code>
 * command.
 */
public class MergeSpeciesCommand extends BasePlugin {

    public MergeSpeciesCommand() {
        super("merge-species", "create a composite cross-species ontology",
                "robot merge-species -i <FILE> -t TAXON [-s SUFFIX] -o <FILE>");
        options.addOption("f", "file", true, "batch file");
        options.addOption("t", "taxon", true, "unfoled for specified taxon");
        options.addOption("p", "property", true, "unfold on specified property");
        options.addOption("s", "suffix", true, "suffix to append to class labels");
        options.addOption("q", "include-property", true, "object property to include");
        options.addOption("x", "extended-translation", false, "enable translation of more class expressions");
        options.addOption("g", "translate-gcas", false, "enable translation of affected general class axioms");
        options.addOption("G", "remove-gcas", false, "remove general class axioms affected by merge");
        options.addOption("d", "remove-declarations", false,
                "enable removal of declaration axioms for translated classes");
        options.addOption("r", "reasoner", true, "reasoner to use");
    }

    @Override
    public void performOperation(CommandState state, CommandLine line) throws Exception {
        List<MergeRun> runs;

        if ( line.hasOption('f') ) {
            runs = parseFile(line.getOptionValue('f'));
        } else {
            runs = new ArrayList<MergeRun>();
            MergeRun mr = new MergeRun();

            if ( !line.hasOption('t') ) {
                throw new IllegalArgumentException("Missing --taxon argument");
            }
            mr.taxonId = getIRI(line.getOptionValue("taxon"), "taxon");
            mr.taxonLabel = line.getOptionValue("s", "species specific");

            if ( line.hasOption("property") ) {
                for ( String property : line.getOptionValues("property") ) {
                    mr.linkProperties.add(getIRI(property, "property"));
                }
            } else {
                mr.linkProperties.add(getIRI("BFO:0000050", "property"));
            }

            if ( line.hasOption("include-property") ) {
                for ( String property : line.getOptionValues("include-property") ) {
                    mr.includedProperties.add(getIRI(property, "include-property"));
                }
            }
            runs.add(mr);
        }

        Date start = Date.from(Instant.now());

        OWLOntology ontology = state.getOntology();
        OWLReasoner reasoner = CommandLineHelper.getReasonerFactory(line).createReasoner(ontology);
        SpeciesMerger merger = new SpeciesMerger(ontology, reasoner);

        if ( line.hasOption('x') ) {
            merger.setExtendedTranslation(true);
        }
        if ( line.hasOption('g') ) {
            merger.setGCAMode(SpeciesMerger.GCAMergeMode.TRANSLATE);
        } else if ( line.hasOption('G') ) {
            merger.setGCAMode(SpeciesMerger.GCAMergeMode.DELETE);
        }
        if ( line.hasOption('d') ) {
            merger.setRemoveDeclarationAxiom(true);
        }

        Date endInit = Date.from(Instant.now());
        System.err.printf("Merge init (%d)\n", endInit.getTime() - start.getTime());

        System.err.printf("Number of runs: %d\n", runs.size());

        for ( MergeRun run : runs ) {
            System.err.printf("Running for %s and properties %s\n", run.taxonId.toQuotedString(), run.linkProperties);
            merger.resetIncludedProperties();
            for ( IRI property : run.includedProperties ) {
                merger.includeProperty(property);
            }

            for ( IRI property : run.linkProperties ) {
                System.err.printf("Unfolding for %s on %s\n", run.taxonId.toQuotedString(), property.toQuotedString());
                Date s1 = Date.from(Instant.now());
                merger.merge(run.taxonId, property, run.taxonLabel);
                Date s2 = Date.from(Instant.now());

                System.err.printf("Merge done (%d)\n", s2.getTime() - s1.getTime());
            }

        }
    }

    private List<MergeRun> parseFile(String file) throws IOException {
        System.err.printf("Reading from %s\n", file);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<MergeRun> runs = new ArrayList<MergeRun>();
        while ( (line = br.readLine()) != null ) {
            System.err.printf("Parsing line [%s]\n", line);
            String[] items = line.split(";");
            if ( items.length < 3 ) {
                continue;
            }

            MergeRun mr = new MergeRun();
            mr.taxonId = getIRI(items[0], "taxon");
            mr.taxonLabel = items[1];
            for ( String p : items[2].split(",") ) {
                System.err.printf("Adding property %s\n", p);
                mr.linkProperties.add(getIRI(p, "property"));
            }
            if ( items.length >= 4 && items[3].length() > 0 ) {
                for ( String p : items[3].split(",") ) {
                    mr.includedProperties.add(getIRI(p, "include-property"));
                }
            }

            runs.add(mr);
        }

        br.close();
        System.err.printf("Number of runs after parse: %d\n", runs.size());

        return runs;
    }

    class MergeRun {
        IRI taxonId;
        String taxonLabel;
        ArrayList<IRI> linkProperties = new ArrayList<IRI>();
        ArrayList<IRI> includedProperties = new ArrayList<IRI>();
    }
}
