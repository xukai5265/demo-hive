package com.tx.demo_hive;
import org.apache.hadoop.hive.ql.exec.UDAF;
import org.apache.hadoop.hive.ql.exec.UDAFEvaluator;

public class UDAF_COL_JSON extends UDAF {
	public static class ConcatUDAFEvaluator implements UDAFEvaluator {
		public static class PartialResult {
			String resultK;
			String result;
			String delimiter=",";
		}

		private PartialResult partial;

		public void init() {
			partial = null;
		}

		public boolean iterate(String value, String deli) {

			if (value == null) {
				return true;
			}
			if (partial == null) {
				partial = new PartialResult();
				partial.result = new String("");
				if (deli == null || deli.equals("")) {
					partial.delimiter = new String(",");
				} else {
					partial.delimiter = new String(deli);
				}

			}
			if (partial.result.length() > 0) {
				partial.result = partial.result.concat(partial.delimiter);
			}

			partial.result = partial.result.concat(value);
			partial.resultK=addK(partial.result);
			return true;
		}

		public PartialResult terminatePartial() {
			partial.resultK=addK(partial.result);
			return partial;
		}

		public boolean merge(PartialResult other) {
			if (other == null) {
				return true;
			}
			if (partial == null) {
				partial = new PartialResult();
				partial.result = new String(other.result);
				partial.delimiter = new String(other.delimiter);
			} else {
				if (partial.result.length() > 0) {
					partial.result = partial.result.concat(partial.delimiter);
				}
				partial.result = partial.result.concat(other.result);
			}
			partial.resultK=addK(partial.result);
			return true;
		}

		public String terminate() {
			return new String(partial.resultK);
		}
		
		
		//减[
		public String minusK(String str){
			
			return str.substring(0, str.length()-1);
		}
		//添加[
		public String addK(String str){
			
			return new StringBuilder("[").append(str).append("]").toString();
		}	
	}
}