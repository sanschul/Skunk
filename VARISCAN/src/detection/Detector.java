package detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.UUID;

import data.Feature;
import data.FeatureExpressionCollection;
import data.FeatureConstant;
import data.Method;
import data.MethodCollection;

// TODO: Auto-generated Javadoc
/**
 * The Class Detector.
 */
public class Detector {

	/**  The config contains the definition of the codesmell. */
	private DetectionConfig config;
	
	/**  Fitting feature locations with an explanation. */
	private Map<FeatureConstant, ArrayList<EnumReason>> featureResult;
	
	/**
	 * Instantiates a new detector.
	 *
	 * @param config the codesmell configuration
	 */
	public Detector (DetectionConfig config)
	{
		this.config = config;
		this.featureResult = new HashMap<FeatureConstant, ArrayList<EnumReason>>();
	}
	
	/**
	 * Perform the detection based on the configuration and return fitting features.
	 *
	 * @return a list with fitting features
	 */
	public Map<FeatureConstant, ArrayList<EnumReason>> Perform()
	{
		System.out.println("... Start detection based on the config file...");
		
		checkFeatureCollection();
		
		checkMethodCollection();
		
		filterResults();
		
		System.out.println("... detection done!");
		
		// return the result
		return this.featureResult;
	}

	
	
	/**
	 * Filter results based on the mandatory values of the configuration.
	 */
	private void filterResults()
	{
		// check for mandatory attributes in the detection configuration
		ArrayList<EnumReason> mandatories = new ArrayList<EnumReason>();
		
		if (config.Feature_MeanLofcRatio_Mand)
			mandatories.add(EnumReason.LARGEFEATURE_LOFCTOMEANLOFC);
		if (config.Feature_ProjectLocRatio_Mand)
			mandatories.add(EnumReason.LARGEFEATURE_LOFCTOLOC);
		if (config.Feature_NoFeatureConstantsRatio_Mand)
			mandatories.add(EnumReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
		if (config.Feature_NumberOfCompilUnits_Mand)
			mandatories.add(EnumReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);
		if (config.Method_LoacToLocRatio_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_LOACTOLOC);
		if (config.Method_LofcToLocRatio_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_LOFCTOLOC);
		if (config.Method_NegationCount_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
		if (config.Method_NestingDepthMin_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
		if (config.Method_NestingSum_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
		if (config.Method_NumberOfFeatureConstantsNonDup_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
		if (config.Method_NumberOfFeatureConstants_Mand)
			mandatories.add(EnumReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTANTS);
		
		// delete featurelocations from the result if it does not contain a mandatory attribute
		ArrayList<FeatureConstant> toDelete = new ArrayList<FeatureConstant>();
		for (FeatureConstant key : featureResult.keySet())
		{
			for (EnumReason mandatory : mandatories)
			{
				if (!featureResult.get(key).contains(mandatory))
					toDelete.add(key);
			}
		}
		
		for (FeatureConstant key : toDelete)
			featureResult.remove(key);
	}
	
	
	/**
	 * Checks the methodlocation for suitable locations in a method.
	 */
	private void checkMethodCollection() {
		for (String file: MethodCollection.methodsPerFile.keySet())
		{
			for (Method meth : MethodCollection.methodsPerFile.get(file))
			{
				// sort functions
				//Collections.sort(meth.featureLocations);
				this.sortByValues(meth.featureConstants);
				
				// ratio lofc to loc
				checkForMethodLofcToLoc(meth);
				
				// ratio loac to loc
				checkForMethodLoacToLoc(meth);
				
				checkMethodForNumberOfFeatureConstants(meth);
				
				checkMethodForNumberOfFeatureLocations(meth);
				
				checkMethodForNumberFeatureConstantsNonDup(meth);
				
				checkMethodForNumberNegations(meth);
				
				checkForMethodNestingSum(meth);
				
				checkForMethodNestingDepthMax(meth);
			}
		}
	}
	
	/**
	 * Check the feature collection for suitable feature locations.
	 */
	private void checkFeatureCollection() {
		// check each feature and location
		for (Feature feat : FeatureExpressionCollection.GetFeatures())
		{	
			checkForFeatureNoFeatureConstantsToSum(feat);
			
			checkForFeatureCompilUnits(feat);
			
			for (FeatureConstant constant : feat.getConstants())
			{
				// check for features that take up a huge part of the project loc
				checkForFeatureToProjectRatio(feat, constant);	
				
				// check for features that are bigger than the mean lofc
				checkForFeatureToFeatureRatio(constant);
			}
		}
	}

	
	
	
	
	/**
	 * Check the ratio between lofc and loc in a method. If the ratio exceeds the configuration value,
	 * add all features with the annotationbundle lofctoloc reason to the result
	 *
	 * @param meth the method
	 */
	private void checkForMethodLofcToLoc(Method meth) {
		if (this.config.Method_LofcToLocRatio != -1000)
		{
			double minLofc = (this.config.Method_LofcToLocRatio * meth.loc);
			
			if(meth.lofc >= minLofc)
			{
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant loc = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(loc, EnumReason.ANNOTATIONBUNDLE_LOFCTOLOC);
				}
			}
		}
	}
	
	/**
	 * Check the ratio between lofa and loc in a method. If the ratio exceeds the configuration value,
	 * add all features with the annotationbundle loactoloc reason to the result
	 *
	 * @param meth the method
	 */
	private void checkForMethodLoacToLoc(Method meth) {
		if (this.config.Method_LoacToLocRatio != -1000)
		{
			double minLoac = (this.config.Method_LoacToLocRatio * meth.loc);
			
			if(meth.GetLinesOfAnnotatedCode() >= minLoac)
			{
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant loc = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(loc, EnumReason.ANNOTATIONBUNDLE_LOACTOLOC);
				}
			}
		}
	}
	
	/**
	 * Check if the number of feature constants in the method exceeds the configuration value.
	 * Add all feature locs to the result with the Number of Feature Constants reason
	 * @param meth the meth
	 */
	private void checkMethodForNumberOfFeatureConstants(Method meth) {
		if (this.config.Method_NumberOfFeatureConstants != -1)
		{
			if (meth.GetFeatureConstantCount() > this.config.Method_NumberOfFeatureConstants)
			{
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(constant, EnumReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTANTS);
				}
			}
		}
	}
	
	/**
	 * Check if the number of feature locations in the method exceeds the configuration value.
	 * Add all feature constans to the result with the Number of Feature Locations reason
	 * @param meth the meth
	 */
	private void checkMethodForNumberOfFeatureLocations(Method meth) {
		if (this.config.Method_NumberOfFeatureLocations != -1)
		{
			if (meth.GetFeatureConstantCount() > this.config.Method_NumberOfFeatureLocations)
			{
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(constant, EnumReason.ANNOTATIONBUNDLE_NUMBERFEATURELOC);
				}
			}
		}
	}

	/**
	 * Check if the number of feature constants in the method exceeds the configuration value.
	 * Add all feature constants to the result with the number of feature constants reason
	 *
	 * @param meth the meth
	 */
	private void checkMethodForNumberFeatureConstantsNonDup(Method meth) {
		if (this.config.Method_NumberOfFeatureConstantsNonDup != -1)
		{
			if (meth.numberFeatureConstantsNonDup > this.config.Method_NumberOfFeatureConstantsNonDup)
			{
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(constant, EnumReason.ANNOTATIONBUNDLE_NUMBERFEATURECONSTNONDUP);
				}
			}
		}
	}
	
	/**
	 * Check method for number negations. If it exceeds the configuration value, add all feature constants
	 * with the specific reason
	 *
	 * @param meth the method
	 */
	private void checkMethodForNumberNegations(Method meth) {
		if (this.config.Method_NegationCount != -1)
		{
			if (meth.negationCount > this.config.Method_NegationCount)
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(constant, EnumReason.ANNOTATIONBUNDLE_NUMBERNEGATIONS);
				}
		}
	}

	/**
	 * Check if the sum of nestings exceeds the code smell configuration value.
	 * If yes, add all feature constants with the corresponding reason to the result.
	 *
	 * @param meth the method
	 */
	private void checkForMethodNestingSum(Method meth) {
		if (this.config.Method_NestingSum != -1)
		{
			if (meth.nestingSum >= this.config.Method_NestingSum)
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					this.addFeatureLocWithReason(constant, EnumReason.ANNOTATIONBUNDLE_NUMBERNESTINGSUM);
				}
		}
	}
	
	/**
	 * Check if the max nesting depth exceeds the code smell configuration value.
	 * If yes, add all feature constant with the corresponding reason to the result.
	 *
	 * @param meth the method
	 */
	private void checkForMethodNestingDepthMax(Method meth) {
		if (this.config.Method_NestingDepthMin != -1)
		{
				// check nesting via stacks and nesting depth
				Stack<FeatureConstant> nestingStack = new Stack<FeatureConstant>();		
				int beginNesting = -1;
				
				for (UUID id : meth.featureConstants.keySet())
				{
					FeatureConstant constant = FeatureExpressionCollection.GetFeatureConstant(meth.featureConstants.get(id), id);
					
					// add the item instantly if the stack is empty, set the beginning nesting depth to the nd of the loc (nesting depth is file-based not method based)
					if (nestingStack.isEmpty())
					{
						beginNesting = constant.nestingDepth;
						nestingStack.push(constant);
					}
					else
					{
						// current nesting in consideration with starting location
						int curNesting = constant.nestingDepth - beginNesting;
						
						// 0 is the beginning nesting degree, everything higher than zero means it is a nested location
						if(curNesting > 0)
							nestingStack.push(constant);
						else
						{
							// calculate nestingdepth of bundle
							int ndm = -1;
							for (FeatureConstant current : nestingStack)
								if ((current.nestingDepth - beginNesting) > ndm)
									ndm = current.nestingDepth - beginNesting;
							
							// if the ndm of the bundle is higher than the configuration add all to the result
							if (ndm >= config.Method_NestingDepthMin)
							{
								while (!nestingStack.isEmpty())
									this.addFeatureLocWithReason(nestingStack.pop(), EnumReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
							}
							else
								nestingStack.empty();
						}
					}
				}
				
				// final emptiing if something is left
				if (!nestingStack.isEmpty())
				{
					// calculate nestingdepth of bundle
					int ndm = -1;
					for (FeatureConstant current : nestingStack)
						if ((current.nestingDepth - beginNesting) > ndm)
							ndm = current.nestingDepth - beginNesting;
					
					if (ndm >= config.Method_NestingDepthMin)
					{
						while (!nestingStack.isEmpty())
							this.addFeatureLocWithReason(nestingStack.pop(), EnumReason.ANNOTATIONBUNDLE_NUMBERNESTINGDEPTHMIN);
					}
					else
						nestingStack.empty();
				}
		}
	}
	
	
	
	
	
	
	/**
	 * Check if the feature constant is bigger than the mean value of feature lofc
	 * Indicates a large feature.
	 *
	 * @param loc the feature constant to examine
	 */
	private void checkForFeatureToFeatureRatio(FeatureConstant loc)
	{
		// if the value is not set, it is -1000
		if (this.config.Feature_MeanLofcRatio != -1000)
		{
			// calculate the minimal lofc a feature location should have to be considered big
			int lofc = (loc.end - loc.start);
			double minLofc = (this.config.Feature_MeanLofcRatio * FeatureExpressionCollection.GetMeanLofc());
			
			// add the feature location if the feature lofc is bigger than the minimal
			if (lofc >= minLofc)
				this.addFeatureLocWithReason(loc, EnumReason.LARGEFEATURE_LOFCTOMEANLOFC);
		}
	}

	/**
	 * Check if the feature takes up a huge percentage of the whole project.
	 * Indicates a large feature.
	 *
	 * @param feat the feature
	 * @param loc the current location
	 */
	private void checkForFeatureToProjectRatio(Feature feat, FeatureConstant loc) 
	{
		// value is set if it is not -1000
		if (this.config.Feature_ProjectLocRatio != -1000)
		{
			// calculate the minimal lofc the feature must have to be a large feature
			double minLofc = (FeatureExpressionCollection.GetLoc() * this.config.Feature_ProjectLocRatio);
			
			// add the feature location
			if (feat.getLofc() >= minLofc)
				this.addFeatureLocWithReason(loc, EnumReason.LARGEFEATURE_LOFCTOLOC);
		}
	}
	
	/**
	 * Check if the feature has more constants than ratio amount.
	 * If yes, add all locs to the result with the corresponding reason.
	 *
	 * @param feat the feat
	 */
	private void checkForFeatureNoFeatureConstantsToSum(Feature feat) 
	{
		if (this.config.Feature_NoFeatureConstantsRatio != -1000)
		{
			// amount of nofls the feature has to exceed for a smell
			double minNofl = FeatureExpressionCollection.numberOfFeatureConstants * this.config.Feature_NoFeatureConstantsRatio;
			
			if (feat.getConstants().size() > minNofl)
			{
				for(FeatureConstant loc : feat.getConstants())
					this.addFeatureLocWithReason(loc, EnumReason.SHOTGUNSURGERY_NOFCOSUMNOFC);
			}
		}
	}

	/**
	 * Check if the feature exceeds the configuration value for compilation units.
	 * If yes, add all constants with the corresponding reason to the result.
	 *
	 * @param feat the feat
	 */
	private void checkForFeatureCompilUnits(Feature feat) 
	{
		if (this.config.Feature_NumberOfCompilUnits != -1)
		{
			if (feat.GetAmountCompilationFiles() > this.config.Feature_NumberOfCompilUnits)
			{
				for(FeatureConstant loc : feat.getConstants())
					this.addFeatureLocWithReason(loc, EnumReason.SHOTGUNSURGERY_NUMBERCOMPILATIONUNITS);
			}
		}
	}
	
	
	
	/**
	 * Adds the feature constant to the result list with the specified reason, or appends another reason if the location is already inside the result list.
	 *
	 * @param constant the feature constant to add
	 * @param reason the reason
	 */
	private void addFeatureLocWithReason(FeatureConstant constant, EnumReason reason)
	{
		if (this.featureResult.containsKey(constant))
			this.featureResult.get(constant).add(reason);
		else
		{
			ArrayList<EnumReason> enumReason = new ArrayList<EnumReason>();
			enumReason.add(reason);
			this.featureResult.put(constant, enumReason);
		}
	}
	
	
	
	
	  /**
  	 * Sort a hashmap by values
  	 *
  	 * @param <K> the key type
  	 * @param <V> the value type
  	 * @param map the map
  	 * @return the linked hash map
  	 */
  	public <K extends Comparable,V extends Comparable> LinkedHashMap<K,V> sortByValues(Map<K,V> map)
  	{
	        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
	      
	        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

	            @Override
	            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
	                return o1.getValue().compareTo(o2.getValue());
	            }
	        });
	        
	        //LinkedHashMap will keep the keys in the order they are inserted
	        //which is currently sorted on natural ordering
	        LinkedHashMap<K,V> sortedMap = new LinkedHashMap<K,V>();
	      
	        for(Map.Entry<K,V> entry: entries){
	            sortedMap.put(entry.getKey(), entry.getValue());
	        }
	      
	        return sortedMap;
	  }
	
}
