/*
 *  Webapplication - Java library that runs on OpenML servers
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package org.openml.webapplication.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.xml.DataFeature.Feature;
import org.openml.webapplication.models.AttributeStatistics;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;

public class ExtractFeatures {
	
	private static final int MAX_SIZE_CLASS_DISTR = 16384;
	
	private static Set<String> checkDataClasses(Instances dataset, String defaultClass) throws Exception {
		if (defaultClass == null) {
			return new TreeSet<String>();
		}
		
		String[] dataClasses = defaultClass.split(",");
		Set<String> classesNotFound = new TreeSet<String>();
		for (String dataClass : dataClasses) {
			if (dataset.attribute(dataClass) == null) {
				classesNotFound.add(dataClass);
			}
		}
		
		if (classesNotFound.size() > 0) {
			throw new Exception("Default target attribute(s) could not be found: " + classesNotFound);
		}
		
		return new TreeSet<String>(Arrays.asList(dataClasses));
	}
	
	public static List<Feature> getFeatures(Instances dataset, String defaultClass) throws Exception {
		Set<String> dataClasses = checkDataClasses(dataset, defaultClass);
		if (dataClasses.size() == 1) {
			dataset.setClass(dataset.attribute(defaultClass));
		}
		
		final ArrayList<Feature> resultFeatures = new ArrayList<Feature>();
		
		for (int i = 0; i < dataset.numAttributes(); i++) {
			Attribute att = dataset.attribute(i);
			Integer numClassValues = null;
			// numClassValues will be null in all cases except for classification datasets
			if (dataset.classIndex() >= 0) {
				if (dataset.classAttribute().isNominal()) {
					numClassValues = dataset.numClasses();
				}
			}
			
			AttributeStatistics attributeStats = new AttributeStatistics(dataset.attribute(i), numClassValues);
		
			for (int j = 0; j < dataset.numInstances(); ++j) {
				if (numClassValues != null) {
					attributeStats.addValue(dataset.get(j).value(i), dataset.get(j).classValue());
				} else {
					attributeStats.addValue(dataset.get(j).value(i));
				}
			}
			
			String data_type = null;
			List<String> nominal_values = new ArrayList<>();
			
			Integer numberOfDistinctValues = null;
			Integer numberOfUniqueValues = null;
			Integer numberOfMissingValues = null;
			Integer numberOfIntegerValues = null;
			Integer numberOfRealValues = null;
			Integer numberOfNominalValues = null;
			Integer numberOfValues = null;
			
			Double maximumValue = null;
			Double minimumValue = null;
			Double meanValue = null;
			Double standardDeviation = null;
				
			AttributeStats as = dataset.attributeStats(i);
				
			numberOfDistinctValues = as.distinctCount;
			numberOfUniqueValues = as.uniqueCount;
			numberOfMissingValues = as.missingCount;
			numberOfIntegerValues = as.intCount;
			numberOfRealValues = as.realCount;
			numberOfMissingValues = as.missingCount;
			
			
			if (att.isNominal()) {
				numberOfNominalValues = att.numValues(); 
			}
			numberOfValues = attributeStats.getTotalObservations();
			
			if (att.isNumeric()) {
				maximumValue = attributeStats.getMaximum();
				minimumValue = attributeStats.getMinimum();
				meanValue = attributeStats.getMean();
				standardDeviation = 0.0;
				try {
					standardDeviation = attributeStats.getStandardDeviation();
				} catch(Exception e) {
					Conversion.log("WARNING", "StdDev", "Could not compute standard deviation of feature "+ att.name() +": "+e.getMessage());
				}
			}
			
			if (att.type() == Attribute.NUMERIC) {
				data_type = "numeric";
			} else if (att.type() == Attribute.NOMINAL) {
				data_type = "nominal";
				for (int j = 0; j < att.numValues(); ++j) {
					nominal_values.add(att.value(j));
				}
			} else if (att.type() == Attribute.STRING) {
				data_type = "string";
			} else if (att.type() == Attribute.DATE) {
				data_type = "date";
			} else {
				data_type = "unknown";
			}
			String classDistr = attributeStats.getClassDistribution();
			if (classDistr.length() > MAX_SIZE_CLASS_DISTR) {
				classDistr = null;
			}
			resultFeatures.add(new Feature(att.index(), att.name(), 
					data_type, nominal_values.toArray(new String[nominal_values.size()]),
					dataClasses.contains(att.name()), 
					numberOfDistinctValues,
					numberOfUniqueValues, numberOfMissingValues,
					numberOfIntegerValues, numberOfRealValues,
					numberOfNominalValues, numberOfValues,
					maximumValue, minimumValue, meanValue,
					standardDeviation, classDistr));
		}
		return resultFeatures;
	}
}
