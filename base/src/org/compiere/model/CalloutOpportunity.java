/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) <Company or Author Name> All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the GNU General Public License       *
 * as published by the Free Software Foundation.                              *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                       *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *                                                                            *
 * @author Adaxa Pty Ltd (Paul Bowden)                                        *
 * FR [ 142 ] Sales Management (CRM)                                          *
 * https://adempiere.atlassian.net/browse/ADEMPIERE-142                       *
 *  			                                                              *
 *****************************************************************************/

package org.compiere.model;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.util.DB;

/**
 * 
 * Sales Opportunity callout
 * @author Paul Bowden, Adaxa Pty Ltd
 * 
 * 
 */
public class CalloutOpportunity extends CalloutEngine {
	
	/**
	 * Update probability based on sales stage
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param mField
	 * @param value
	 * @return
	 */
	public String salesStage (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive() || value == null)
			return "";
	
		int C_SalesStage_ID = (Integer) value;
		
		String sql = "SELECT Probability FROM C_SalesStage WHERE C_SalesStage_ID = ?";
		BigDecimal probability = DB.getSQLValueBD(null, sql, C_SalesStage_ID);
		if ( probability != null )
			mTab.setValue("Probability", probability);
		
		return "";
	}
	
		
	

}
