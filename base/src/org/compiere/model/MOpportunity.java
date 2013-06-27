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

import java.sql.ResultSet;
import java.util.Properties;

public class MOpportunity extends X_C_Opportunity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9052544341602655427L;

	public MOpportunity(Properties ctx, int C_Opportunity_ID, String trxName) {
		super(ctx, C_Opportunity_ID, trxName);
	}

	public MOpportunity(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if ( getC_Order_ID() > 0 )
		{
			I_C_Order order = getC_Order();
			if ( order != null )
				setOpportunityAmt(order.getGrandTotal());
		}
		return true;
	}

}
