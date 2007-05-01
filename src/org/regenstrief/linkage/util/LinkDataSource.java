package org.regenstrief.linkage.util;

/*
 * Class holds the column information for the linkage data source.
 * The columns are referenced by the index of the columns in the
 * data source, since that index will not change.
 */

import java.util.*;

public class LinkDataSource {
	private String source_name;
	private String type;
	private String access;
	private int n_records;
	
	public static final int UNKNOWN_REC_COUNT = -1;
	
	private List<DataColumn> column_settings;
	
	public LinkDataSource(String name, String type, String access){
		this.source_name = name;
		this.type = type;
		this.access = access;
		column_settings = new ArrayList<DataColumn>();
		n_records = UNKNOWN_REC_COUNT;
	}
	
	public LinkDataSource(String name, String type, String access, int n_records){
		this.source_name = name;
		this.type = type;
		this.access = access;
		column_settings = new ArrayList<DataColumn>();
		this.n_records = n_records;
	}
	
	/*
	 * Method added to conveniently go from a list of columns by name,
	 * such as would be returned from a matching config object, and
	 * returns the display position of them.  Method uses lds1
	 * since lds2 might not be present if linking between same file
	 */
	public int[] getIndexesOfColumnNames(String[] names){
		int[] ret = new int[names.length];
		for(int i = 0; i < names.length; i++){
			ret[i] = getDisplayPositionByName(names[i]);
		}
		return ret;
	}
	
	public void setSourceName(String name){
		source_name = name;
	}
	
	public String getName(){
		return source_name;
	}
	
	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	public String getType() {
		return type;
	}
	
	public int getDisplayPositionByName(String name){
		Iterator<DataColumn> it = column_settings.iterator();
		while(it.hasNext()){
			DataColumn dc = it.next();
			if(dc.getName().equals(name)){
				return dc.getIncludePosition();
			}
		}
		return DataColumn.INCLUDE_NA;
	}
	
	public int getColumnTypeByName(String name){
		Iterator<DataColumn> it = column_settings.iterator();
		while(it.hasNext()){
			DataColumn dc = it.next();
			if(dc.getName().equals(name)){
				return dc.getType();
			}
		}
		return DataColumn.STRING_TYPE;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public DataColumn getDataColumn(int data_position){
		return column_settings.get(data_position);
	}
	
	public void addDataColumn(DataColumn dc){
		column_settings.add(dc);
	}
	
	public void addNewDataColumn(String col_id){
		DataColumn dc = new DataColumn(col_id);
		addDataColumn(dc);
	}
	
	public List<DataColumn> getDataColumns(){
		return column_settings;
	}
	
	/*
	 * returns the number of columns that are displayed and re-written
	 * to the new file for linkage
	 */
	public int getIncludeCount(){
		Iterator<DataColumn> it = column_settings.iterator();
		int total = 0;
		while(it.hasNext()){
			DataColumn dc = it.next();
			if(dc.getIncludePosition() != DataColumn.INCLUDE_NA){
				total++;
			}
		}
		return total;
	}
	
	public void setRecordCount(int count){
		n_records = count;
	}
	
	public int getRecordCount(){
		return n_records;
	}
	
	public Hashtable<String, Integer> getTypeTable(){
		Hashtable<String, Integer> type_table = new Hashtable<String, Integer>();
		List<DataColumn> dc = getDataColumns();
		Iterator<DataColumn> it = dc.iterator();
		while(it.hasNext()){
			DataColumn d = it.next();
			if(d.getIncludePosition() != DataColumn.INCLUDE_NA){
				type_table.put(d.getName(), new Integer(d.getType()));
			}
		}
		return type_table;
	}
}
