package org.regenstrief.linkage.io;

import java.util.ArrayList;
import java.util.Iterator;

import org.regenstrief.linkage.util.DataColumn;
import org.regenstrief.linkage.util.LinkDataSource;
import org.regenstrief.linkage.util.MatchingConfig;

/**
 * Class extends DataBaseReader by taking a MatchingConfig object in the 
 * constructor providng Record order information in its blocking variable
 * information.  A different result set is obtained by overriding the
 * constructQuery() method.
 *
 */

public class OrderedDataBaseReader extends DataBaseReader {
	
	private MatchingConfig mc;
	
	/**
	 * Constructs a reader, but returns the Records in order specified by the
	 * blocking variables.
	 * 
	 * @param lds	the description of the data
	 * @param mc	information on the record linkage options, containing blocking variable order (sort order)
	 */
	public OrderedDataBaseReader(LinkDataSource lds, MatchingConfig mc){
		super(lds);
		this.mc = mc;
	}
	
	public String constructQuery(){
		String query = new String("SELECT ");
		incl_cols = new ArrayList<DataColumn>();
		Iterator<DataColumn> it = data_source.getDataColumns().iterator();
		while(it.hasNext()){
			DataColumn dc = it.next();
			if(dc.getIncludePosition() != DataColumn.INCLUDE_NA){
				incl_cols.add(dc);
			}
		}
		
		for(int i = 0; i < incl_cols.size() - 1; i++){
			query += incl_cols.get(i).getName() + ", ";
		}
		
		query += incl_cols.get(incl_cols.size() - 1).getName();
		query += " FROM " + data_source.getName();
		query += " ORDER BY ";
		String[] b_columns = mc.getBlockingColumns();
		for(int i = 0; i < b_columns.length - 1; i++){
			query += b_columns[i] + ", ";
		}
		query += b_columns[b_columns.length - 1];
		return query;
	}
}
