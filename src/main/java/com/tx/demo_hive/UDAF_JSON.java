package com.tx.demo_hive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFCollectList;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StandardListObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tx.demo_hive.GenericUDAFMkCollectionEvaluator.BufferType;
/**
 * 
 * @author mu
 *
 */
@Description(name = "collect_list", value = "_FUNC_(x) - Returns a list of objects with duplicates")
public class UDAF_JSON extends AbstractGenericUDAFResolver {

	static final Log LOG = LogFactory.getLog(GenericUDAFCollectList.class.getName());

	public UDAF_JSON() {
	}

	@Override
	public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters) throws SemanticException {
		if (parameters.length != 1) {
			throw new UDFArgumentTypeException(parameters.length - 1, "Exactly one argument is expected.");
		}
		if (parameters[0].getCategory() != ObjectInspector.Category.PRIMITIVE) {
			throw new UDFArgumentTypeException(0, "Only primitive type arguments are accepted but "
					+ parameters[0].getTypeName() + " was passed as parameter 1.");
		}
		return new GenericUDAFMkCollectionEvaluator(GenericUDAFMkCollectionEvaluator.BufferType.LIST);
	}

	
}
class GenericUDAFMkCollectionEvaluator extends GenericUDAFEvaluator implements Serializable {

	private static final long serialVersionUID = 1l;
	static final Log LOG = LogFactory.getLog(GenericUDAFMkCollectionEvaluator.class.getName());

	enum BufferType {
		SET, LIST
	}

	// For PARTIAL1 and COMPLETE: ObjectInspectors for original data
	private transient PrimitiveObjectInspector inputOI;
	// For PARTIAL2 and FINAL: ObjectInspectors for partial aggregations
	// (list
	// of objs)
	private transient StandardListObjectInspector loi;

	private transient ListObjectInspector internalMergeOI;

	private BufferType bufferType;

	// needed by kyro
	public GenericUDAFMkCollectionEvaluator() {
	}

	public GenericUDAFMkCollectionEvaluator(BufferType bufferType) {
		this.bufferType = bufferType;
	}

	@Override
	public ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException {
		super.init(m, parameters);
		// init output object inspectors
		// The output of a partial aggregation is a list
		if (m == Mode.PARTIAL1) {
			inputOI = (PrimitiveObjectInspector) parameters[0];
			return ObjectInspectorFactory.getStandardListObjectInspector(
					(PrimitiveObjectInspector) ObjectInspectorUtils.getStandardObjectInspector(inputOI));
		} else {
			if (!(parameters[0] instanceof ListObjectInspector)) {
				// no map aggregation.
				inputOI = (PrimitiveObjectInspector) ObjectInspectorUtils.getStandardObjectInspector(parameters[0]);
				return (StandardListObjectInspector) ObjectInspectorFactory.getStandardListObjectInspector(inputOI);
			} else {
				internalMergeOI = (ListObjectInspector) parameters[0];
				inputOI = (PrimitiveObjectInspector) internalMergeOI.getListElementObjectInspector();
				loi = (StandardListObjectInspector) ObjectInspectorUtils
						.getStandardObjectInspector(internalMergeOI);
				return loi;
			}
		}
	}

	class MkArrayAggregationBuffer extends AbstractAggregationBuffer {

		private Collection<Object> container;

		public MkArrayAggregationBuffer() {
			if (bufferType == BufferType.LIST) {
				container = new ArrayList<Object>();
			} else if (bufferType == BufferType.SET) {
				container = new LinkedHashSet<Object>();
			} else {
				throw new RuntimeException("Buffer type unknown");
			}
		}
	}

	@Override
	public void reset(AggregationBuffer agg) throws HiveException {
		((MkArrayAggregationBuffer) agg).container.clear();
	}

	@Override
	public AggregationBuffer getNewAggregationBuffer() throws HiveException {
		MkArrayAggregationBuffer ret = new MkArrayAggregationBuffer();
		return ret;
	}

	// mapside
	@Override
	public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
		assert (parameters.length == 1);
		Object p = parameters[0];

		if (p != null) {
			MkArrayAggregationBuffer myagg = (MkArrayAggregationBuffer) agg;
			putIntoCollection(p, myagg);
		}
	}

	// mapside
	@Override
	public Object terminatePartial(AggregationBuffer agg) throws HiveException {
		MkArrayAggregationBuffer myagg = (MkArrayAggregationBuffer) agg;

		LOG.warn("自定义terminatePartial："+myagg.container);
		List<Object> ret = new ArrayList<Object>(myagg.container.size());
		ret.addAll(myagg.container);
		return ret;
	}

	@Override
	public void merge(AggregationBuffer agg, Object partial) throws HiveException {
		MkArrayAggregationBuffer myagg = (MkArrayAggregationBuffer) agg;
		LOG.warn("自定义merge："+myagg.container);
		List<Object> partialResult = (ArrayList<Object>) internalMergeOI.getList(partial);
		if (partialResult != null) {
			for (Object i : partialResult) {
				putIntoCollection(i, myagg);
			}
		}
	}

	@Override
	public Object terminate(AggregationBuffer agg) throws HiveException {
		MkArrayAggregationBuffer myagg = (MkArrayAggregationBuffer) agg;
//		List<Object> ret = new ArrayList<Object>(myagg.container.size());
//		ret.addAll(myagg.container);
		List<Object> result = new ArrayList<Object>(myagg.container.size());
		LOG.warn("自定义terminate："+myagg.container);
		for(Object obj:myagg.container){
			if(obj.toString().startsWith("{")||obj.toString().startsWith("["))
				result.add(JSON.parseObject(obj.toString())); 
		}
		return result;
	}

	private void putIntoCollection(Object p, MkArrayAggregationBuffer myagg) {
		Object pCopy = ObjectInspectorUtils.copyToStandardObject(p, this.inputOI);
		myagg.container.add(pCopy);
	}

	public BufferType getBufferType() {
		return bufferType;
	}

	public void setBufferType(BufferType bufferType) {
		this.bufferType = bufferType;
	}

}