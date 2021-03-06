package tuffy.main;

import java.util.ArrayList;

import tuffy.infer.MRF;
import tuffy.infer.MRF;
import tuffy.infer.ds.GClause;
import tuffy.parse.CommandOptions;
import tuffy.util.BitSetIntPair;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.Stats;
import tuffy.util.Timer;
import tuffy.util.UIMan;
/**
 * Non-parition-aware inference.
 */
public class NonPartInfer extends Infer{
	public void run(CommandOptions opt){
		try {
		
			UIMan.println(">>> Running non-partition inference.");
			setUp(opt);
			Timer.start("groundOverall");
			UIMan.println(">>> Starting grounding...");
			ground();
			UIMan.println("### total grounding = " + Timer.elapsed("groundOverall"));
			
			if (Timer.hasTimedOut()) {
				ExceptionMan.die("Tuffy timed out");
			}
	
			if(options.maxFlips == 0){
				options.maxFlips = 100 * grounding.getNumAtoms();
			}
			if(options.maxTries == 0){
				options.maxTries = 3;
			}
	
			MRF mrf = null;
			
//			if((!opt.marginal && !opt.mle) || opt.dual){
//				UIMan.println(">>> Running MAP inference...");
//				String mapfout = options.fout;
//				if(opt.dual) mapfout += ".map";
//	
//				UIMan.println("    Loading MRF from DB to RAM...");
//				mrf = dmover.loadSimpleMrfFromDb(mln.relAtoms, mln.relClauses);
//				mrf.inferWalkSAT(options.maxTries, options.maxFlips);
//				dmover.flushAtomStates(mrf.atoms.values(), mln.relAtoms, true);
//				
//				UIMan.println("### Best answer has cost " + UIMan.decimalRound(2,mrf.lowCost));
//				UIMan.println(">>> Writing answer to file: " + mapfout);
//				dmover.dumpTruthToFile(mln.relAtoms, mapfout);
//			}
	
			if((opt.marginal && !opt.mle) || opt.dual){
				UIMan.println(">>> Running marginal inference...");
				String mfout = options.fout;
				if(opt.dual) mfout += ".marginal";
				
				if(mrf == null){
					mrf = new MRF(mln);
					UIMan.verbose(3, "Loading MRF from DB...");
					dmover.loadMrfFromDb(mrf, mln.relAtoms, mln.relClauses);
				}
							
				if (Config.unitPropagate) {
					Timer.start("fullUnitPropagate");
					UIMan.println(">>> Starting unit propagation...");
					mrf = mrf.unitPropagateAndGetNewMRF();
					dmover.writeMRFClausesToTable(mrf, mln.relClauses);
					writeClausesToFile();
					writeWCNFToFile();
					UIMan.println("### MRF Size After Unit Prop: atoms = " + mrf.atoms.size() + "; clauses = " + mrf.clauses.size());
					Stats.javaUPGroundingTimeMs += Timer.elapsedMilliSeconds("fullUnitPropagate");
					UIMan.println("### total unit propagation = " + Timer.elapsed("fullUnitPropagate"));
				}
				
//				Stats.numberGroundAtoms = mrf.atoms.size();
//				Stats.numberGroundClauses = mrf.clauses.size();
//				int numberUnits = 0;
//				for (GClause gc : mrf.clauses) {
//					if (gc.isUnitClause() && gc.isHardClause()) {
//						numberUnits++;
//					}
//				}
//				Stats.numberUnits = numberUnits;
				MRF.computeStats(mrf);
				
				Timer.start("mcsat");
				UIMan.println(">>>Starting MC-Sat...");
				double sumCost = mrf.mcsat(options.mcsatSamples, options.maxFlips);
				UIMan.println("### total mcsat = " + Timer.elapsed("mcsat"));
				dmover.flushAtomStates(mrf.atoms.values(), mln.relAtoms);
		
				UIMan.println("### Average Cost = " + UIMan.decimalRound(2,sumCost/options.mcsatSamples));
				
				UIMan.println(">>> Writing answer to file: " + mfout);
				dmover.dumpProbsToFile(mln.relAtoms, mfout);

				UIMan.println(Stats.numberGroundClauses +"");
				UIMan.println(Stats.numberUnits +"");
				UIMan.println(Stats.numberGroundAtoms +"");
				UIMan.println(Stats.numberSamplesAtTimeout+"");
				UIMan.println(Stats.numberClausesAtTimeout+"");
				UIMan.println(Stats.glucoseTimeMs+"");
				UIMan.println(Stats.javaUPGroundingTimeMs +"");
				UIMan.println(Stats.mcsatStepsWhereSampleSatFails +"");
				
				UIMan.println("WalkSAT Random Step Prob: (-randomStep)" + Config.walksat_random_step_probability);
				UIMan.println("Simulated Annealing Step Prob: (-saProb)" + Config.simulatedAnnealingSampleSATProb);
				UIMan.println("Simulated Annealing inverse temp: (-sa)" + Config.samplesat_sa_coef);
				UIMan.println("Number MC-SAT Samples: (-mcsatSamples)" + options.mcsatSamples);
				UIMan.println("Number Flips: (-maxFlips)" + options.maxFlips);
			}
			
//			if(opt.mle){
//				UIMan.println(">>> Running MLE inference...");
//				String mfout = options.fout;
//				if(opt.dual) mfout += ".mle";
//				
//				if(mrf == null){
//					mrf = new MRF(mln);
//					dmover.loadMrfFromDb(mrf, mln.relAtoms, mln.relClauses);
//				}
//				
//				ArrayList<BitSetIntPair> mle_rs_cache = new ArrayList<BitSetIntPair>();
//				double sumCost = mrf.MLE_naiveSampler(options.mcsatSamples);
//				
//				dmover.flushAtomStates(mrf.atoms.values(), mln.relAtoms, true);
//		
//				UIMan.println("### Prob = " + UIMan.decimalRound(2,sumCost));
//				
//				UIMan.println(">>> Writing answer to file: " + mfout);
//				dmover.dumpTruthToFile(mln.relAtoms, mfout);
//				
//				int solutionid = 0;
//				for(BitSetIntPair rs : mle_rs_cache){
//					dmover.flushAtomStatesFromBitMap(mrf.atoms.values(), rs.bitset, mln.relAtoms, 1.0*rs.integer, "mle_rs_" + solutionid, true);
//					solutionid ++;
//					if(solutionid > 50){
//						break;
//					}
//				}
//				
//				dmover.dumpMLETruthToFile(mfout);
//				
//			}
			
			cleanUp();
		} catch (Error e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			db.close();
			UIMan.println(Stats.numberGroundClauses +"");
			UIMan.println(Stats.numberUnits +"");
			UIMan.println(Stats.numberGroundAtoms +"");
			UIMan.println(Stats.numberSamplesAtTimeout+"");
			UIMan.println(Stats.numberClausesAtTimeout+"");
			UIMan.println(Stats.glucoseTimeMs+"");
			UIMan.println(Stats.javaUPGroundingTimeMs +"");
			UIMan.println(Stats.mcsatStepsWhereSampleSatFails +"");
			
			UIMan.println("WalkSAT Random Step Prob: (-randomStep)" + Config.walksat_random_step_probability);
			UIMan.println("Simulated Annealing Step Prob: (-saProb)" + Config.simulatedAnnealingSampleSATProb);
			UIMan.println("Simulated Annealing inverse temp: (-sa)" + Config.samplesat_sa_coef);
			UIMan.println("Number MC-SAT Samples: (-mcsatSamples)" + options.mcsatSamples);
			UIMan.println("Number Flips: (-maxFlips)" + options.maxFlips);
			
			throw e1;
		}
	}

}


