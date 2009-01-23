/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
/**
 *	Inventory Movement Model
 *	
 *  @author Jorg Janke
 *  @author victor.perez@-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li>FR [ 1948157  ]  Is necessary the reference for document reverse
 *  		@see http://sourceforge.net/tracker/?func=detail&atid=879335&aid=1948157&group_id=176962
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *  @author Armen Rizal, Goodwill Consulting
 * 			<li>BF [ 1745154 ] Cost in Reversing Material Related Docs  
 *  @version $Id: MMovement.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MMovement extends X_M_Movement implements DocAction
{
	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_Movement_ID id
	 *	@param trxName transaction
	 */
	public MMovement (Properties ctx, int M_Movement_ID, String trxName)
	{
		super (ctx, M_Movement_ID, trxName);
		if (M_Movement_ID == 0)
		{
		//	setC_DocType_ID (0);
			setDocAction (DOCACTION_Complete);	// CO
			setDocStatus (DOCSTATUS_Drafted);	// DR
			setIsApproved (false);
			setIsInTransit (false);
			setMovementDate (new Timestamp(System.currentTimeMillis()));	// @#Date@
			setPosted (false);
			super.setProcessed (false);
		}	
	}	//	MMovement

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MMovement (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MMovement

	/**	Lines						*/
	private MMovementLine[]		m_lines = null;
	/** Confirmations				*/
	private MMovementConfirm[]	m_confirms = null;
	
	/**
	 * 	Get Lines
	 *	@param requery requery
	 *	@return array of lines
	 */
	public MMovementLine[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		String whereClause = "M_Movement_ID=?";
		List<MMovement> list = new Query(getCtx(), MMovementLine.Table_Name, whereClause, null)
		 										.setParameters(new Object[]{getM_Movement_ID()})
		 										.setOrderBy(MMovementLine.COLUMNNAME_Line)
		 										.list();
		m_lines = new MMovementLine[list.size ()];
		list.toArray (m_lines);
		return m_lines;
	}	//	getLines

	/**
	 * 	Get Confirmations
	 * 	@param requery requery
	 *	@return array of Confirmations
	 */
	public MMovementConfirm[] getConfirmations(boolean requery)
	{
		if (m_confirms != null && !requery)
			return m_confirms;

		ArrayList<MMovementConfirm> list = new ArrayList<MMovementConfirm>();
		String sql = "SELECT * FROM M_MovementConfirm WHERE M_Movement_ID=?";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getM_Movement_ID());
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add(new MMovementConfirm(getCtx(), rs, get_TrxName()));
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "getConfirmations", e);
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			pstmt = null;
		}
		
		m_confirms = new MMovementConfirm[list.size ()];
		list.toArray (m_confirms);
		return m_confirms;
	}	//	getConfirmations

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
	//	ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
	//	if (re == null)
			return null;
	//	return re.getPDF(file);
	}	//	createPDF

	
	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (getC_DocType_ID() == 0)
		{
			MDocType types[] = MDocType.getOfDocBaseType(getCtx(), MDocType.DOCBASETYPE_MaterialMovement);
			if (types.length > 0)	//	get first
				setC_DocType_ID(types[0].getC_DocType_ID());
			else
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@NotFound@ @C_DocType_ID@"));
				return false;
			}
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	Set Processed.
	 * 	Propergate to Lines/Taxes
	 *	@param processed processed
	 */
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		String sql = "UPDATE M_MovementLine SET Processed='"
			+ (processed ? "Y" : "N")
			+ "' WHERE M_Movement_ID=" + getM_Movement_ID();
		int noLine = DB.executeUpdate(sql, get_TrxName());
		m_lines = null;
		log.fine("Processed=" + processed + " - Lines=" + noLine);
	}	//	setProcessed
	
	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info(toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		log.info(toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	public String prepareIt()
	{
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getMovementDate(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		MMovementLine[] lines = getLines(false);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		//	Add up Amounts
		
		
		//checkMaterialPolicy();
		
		//	Confirmation
		if (dt.isInTransit())
			createConfirmation();

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * 	Create Movement Confirmation
	 */
	private void createConfirmation()
	{
		MMovementConfirm[] confirmations = getConfirmations(false);
		if (confirmations.length > 0)
			return;
		
		//	Create Confirmation
		MMovementConfirm.create (this, false);
	}	//	createConfirmation
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		log.info(toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		log.info(toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Outstanding (not processed) Incoming Confirmations ?
		MMovementConfirm[] confirmations = getConfirmations(true);
		for (int i = 0; i < confirmations.length; i++)
		{
			MMovementConfirm confirm = confirmations[i];
			if (!confirm.isProcessed())
			{
				m_processMsg = "Open: @M_MovementConfirm_ID@ - " 
					+ confirm.getDocumentNo();
				return DocAction.STATUS_InProgress;
			}
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info(toString());
		
		//
		MMovementLine[] lines = getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			MMovementLine line = lines[i];
			MTransaction trxFrom = null; 
			
			//Stock Movement - Counterpart MOrder.reserveStock
			MProduct product = line.getProduct();
			if (product != null 
				&& product.isStocked() )
			{
				//Ignore the Material Policy when is Reverse Correction
				if(!isReversal())
				checkMaterialPolicy(line);
				
					if (line.getM_AttributeSetInstance_ID() == 0)
					{
						MMovementLineMA mas[] = MMovementLineMA.get(getCtx(),
							line.getM_MovementLine_ID(), get_TrxName());
						for (int j = 0; j < mas.length; j++)
						{
							MMovementLineMA ma = mas[j];
							//
							/*MStorage storageFrom = MStorage.get(getCtx(), line.getM_Locator_ID(), 
								line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(), get_TrxName());
							if (storageFrom == null)
								storageFrom = MStorage.getCreate(getCtx(), line.getM_Locator_ID(), 
									line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(), get_TrxName());
							//*/
							MLocator locator = new MLocator (getCtx(), line.getM_Locator_ID(), get_TrxName());
							//Update Storage 
							if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
								line.getM_Locator_ID(),
								line.getM_Product_ID(), 
								ma.getM_AttributeSetInstance_ID(), 0, 
								ma.getMovementQty().negate(), Env.ZERO ,  Env.ZERO , get_TrxName()))
							{
								m_processMsg = "Cannot correct Inventory (MA)";
								return DocAction.STATUS_Invalid;
							}
							
							/*MStorage storageTo = MStorage.get(getCtx(), line.getM_LocatorTo_ID(), 
								line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(), get_TrxName());
							if (storageTo == null)
								storageTo = MStorage.getCreate(getCtx(), line.getM_LocatorTo_ID(), 
									line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(), get_TrxName());
							//*/
							
							// MLocator locatorTo = new MLocator (getCtx(), line.getM_LocatorTo_ID(), get_TrxName());
							//Update Storage 
							if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
								line.getM_LocatorTo_ID(),
								line.getM_Product_ID(), 
								ma.getM_AttributeSetInstance_ID(), 0, 
								ma.getMovementQty(), Env.ZERO ,  Env.ZERO , get_TrxName()))
							{
								m_processMsg = "Cannot correct Inventory (MA)";
								return DocAction.STATUS_Invalid;
							}
							
							/*storageFrom.setQtyOnHand(storageFrom.getQtyOnHand().subtract(ma.getMovementQty()));
							if (!storageFrom.save(get_TrxName()))
							{
								m_processMsg = "Storage From not updated (MA)";
								return DocAction.STATUS_Invalid;
							}
							//
							storageTo.setQtyOnHand(storageTo.getQtyOnHand().add(ma.getMovementQty()));
							if (!storageTo.save(get_TrxName()))
							{
								m_processMsg = "Storage To not updated (MA)";
								return DocAction.STATUS_Invalid;
							}*/
						
							//
							trxFrom = new MTransaction (getCtx(), line.getAD_Org_ID(), 
								MTransaction.MOVEMENTTYPE_MovementFrom,
								line.getM_Locator_ID(), line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
								ma.getMovementQty().negate(), getMovementDate(), get_TrxName());
							trxFrom.setM_MovementLine_ID(line.getM_MovementLine_ID());
							if (!trxFrom.save())
							{
								m_processMsg = "Transaction From not inserted (MA)";
								return DocAction.STATUS_Invalid;
							}
							//
							MTransaction trxTo = new MTransaction (getCtx(), line.getAD_Org_ID(), 
								MTransaction.MOVEMENTTYPE_MovementTo,
								line.getM_LocatorTo_ID(), line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
								ma.getMovementQty(), getMovementDate(), get_TrxName());
							trxTo.setM_MovementLine_ID(line.getM_MovementLine_ID());
							if (!trxTo.save())
							{
								m_processMsg = "Transaction To not inserted (MA)";
								return DocAction.STATUS_Invalid;
							}
						}
					}
					//	Fallback - We have ASI
					if (trxFrom == null)
					{
						/*MStorage storageFrom = MStorage.get(getCtx(), line.getM_Locator_ID(), 
							line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(), get_TrxName());
						if (storageFrom == null)
							storageFrom = MStorage.getCreate(getCtx(), line.getM_Locator_ID(), 
								line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(), get_TrxName());
						//
						MStorage storageTo = MStorage.get(getCtx(), line.getM_LocatorTo_ID(), 
							line.getM_Product_ID(), line.getM_AttributeSetInstanceTo_ID(), get_TrxName());
						if (storageTo == null)
							storageTo = MStorage.getCreate(getCtx(), line.getM_LocatorTo_ID(), 
								line.getM_Product_ID(), line.getM_AttributeSetInstanceTo_ID(), get_TrxName());
						//
						storageFrom.setQtyOnHand(storageFrom.getQtyOnHand().subtract(line.getMovementQty()));
						if (!storageFrom.save(get_TrxName()))
						{
							m_processMsg = "Storage From not updated";
							return DocAction.STATUS_Invalid;
						}
						//
						storageTo.setQtyOnHand(storageTo.getQtyOnHand().add(line.getMovementQty()));
						if (!storageTo.save(get_TrxName()))
						{
							m_processMsg = "Storage To not updated";
							return DocAction.STATUS_Invalid;
						}*/
					
						MLocator locator = new MLocator (getCtx(), line.getM_Locator_ID(), get_TrxName());
						//Update Storage 
						if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
							line.getM_Locator_ID(),
							line.getM_Product_ID(), 
							line.getM_AttributeSetInstance_ID(), 0, 
							line.getMovementQty().negate(), Env.ZERO ,  Env.ZERO , get_TrxName()))
						{
							m_processMsg = "Cannot correct Inventory (MA)";
							return DocAction.STATUS_Invalid;
						}
						
						// MLocator locatorTo = new MLocator (getCtx(), line.getM_LocatorTo_ID(), get_TrxName());
						//Update Storage 
						if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
							line.getM_LocatorTo_ID(),
							line.getM_Product_ID(), 
							line.getM_AttributeSetInstance_ID(), 0, 
							line.getMovementQty(), Env.ZERO ,  Env.ZERO , get_TrxName()))
						{
							m_processMsg = "Cannot correct Inventory (MA)";
							return DocAction.STATUS_Invalid;
						}
						
						//
						trxFrom = new MTransaction (getCtx(), line.getAD_Org_ID(), 
							MTransaction.MOVEMENTTYPE_MovementFrom,
							line.getM_Locator_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
							line.getMovementQty().negate(), getMovementDate(), get_TrxName());
						trxFrom.setM_MovementLine_ID(line.getM_MovementLine_ID());
						if (!trxFrom.save())
						{
							m_processMsg = "Transaction From not inserted";
							return DocAction.STATUS_Invalid;
						}
						//
						MTransaction trxTo = new MTransaction (getCtx(), line.getAD_Org_ID(), 
							MTransaction.MOVEMENTTYPE_MovementTo,
							line.getM_LocatorTo_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstanceTo_ID(),
							line.getMovementQty(), getMovementDate(), get_TrxName());
						trxTo.setM_MovementLine_ID(line.getM_MovementLine_ID());
						if (!trxTo.save())
						{
							m_processMsg = "Transaction To not inserted";
							return DocAction.STATUS_Invalid;
						}
					}	//	Fallback
					
						// update Distribution Order Line
						if(line.getDD_OrderLine_ID() > 0)
						{
						   MDDOrderLine oline= new MDDOrderLine(getCtx(),line.getDD_OrderLine_ID(), get_TrxName());
						   MLocator locator_to = MLocator.get(getCtx(), line.getM_LocatorTo_ID());
						   MWarehouse warehouse =  MWarehouse.get(getCtx(), locator_to.getM_Warehouse_ID()); 
						   if(warehouse.isInTransit())
						   {
							   oline.setQtyInTransit(oline.getQtyInTransit().add(line.getMovementQty()));
							   oline.setConfirmedQty(Env.ZERO);
						   }
						   else
						   {
							   oline.setQtyInTransit(oline.getQtyInTransit().subtract(line.getMovementQty()));
							   oline.setQtyDelivered(oline.getQtyDelivered().add(line.getMovementQty()));
						   }   
						   oline.save();
						}
				  } // product stock	
				}	//	for all lines
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		
		// Set Distribution Order InTransit
		
		if(getDD_Order_ID() > 0)
		{	
			MDDOrder order = new MDDOrder(getCtx(),getDD_Order_ID(),get_TrxName());
			order.setIsInTransit(true);
			order.save();
		}	
		
		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		//
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setMovementDate(new Timestamp (System.currentTimeMillis()));
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (value != null)
				setDocumentNo(value);
		}
	}

	/**
	 * 	Check Material Policy
	 * 	Sets line ASI
	 */
	private void checkMaterialPolicy(MMovementLine line)
	{
		int no = MMovementLineMA.deleteMovementMA(getM_Movement_ID(), get_TrxName());
		if (no > 0)
			log.config("Delete old #" + no);
		//MMovementLine[] lines = getLines(false);
		
		//	Check Lines
		//for (int i = 0; i < lines.length; i++)
		//{
		//	MMovementLine line = lines[i];
			boolean needSave = false;
			BigDecimal qtyASI = Env.ZERO ;

			//	Attribute Set Instance
			if (line.getM_AttributeSetInstance_ID() == 0)
			{
				MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
				String MMPolicy = product.getMMPolicy();
				MStorage[] storages = MStorage.getAllWithASI(getCtx(), 
					line.getM_Product_ID(),	line.getM_Locator_ID(), 
					MClient.MMPOLICY_FiFo.equals(MMPolicy), get_TrxName());
				BigDecimal qtyToDeliver = line.getMovementQty();
				
				/*for (int ii = 0; ii < storages.length; ii++)
				{
					MStorage storage = storages[ii];
					if (ii == 0)
					{
						if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
						{
							line.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
							needSave = true;
							log.config("Direct - " + line);
							qtyToDeliver = Env.ZERO;
						}
						else
						{
							log.config("Split - " + line);
							MMovementLineMA ma = new MMovementLineMA (line, 
								storage.getM_AttributeSetInstance_ID(),
								storage.getQtyOnHand());
							if (!ma.save())
								;
							qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
							log.fine("#" + ii + ": " + ma + ", QtyToDeliver=" + qtyToDeliver);
						}
					}
					else	//	 create addl material allocation
					{
						MMovementLineMA ma = new MMovementLineMA (line, 
							storage.getM_AttributeSetInstance_ID(),
							qtyToDeliver);
						if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
							qtyToDeliver = Env.ZERO;
						else
						{
							ma.setMovementQty(storage.getQtyOnHand());
							qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
						}
						if (!ma.save())
							;
						log.fine("#" + ii + ": " + ma + ", QtyToDeliver=" + qtyToDeliver);
					}
					if (qtyToDeliver.signum() == 0)
						break;
				}	//	 for all storages
					
				//	No AttributeSetInstance found for remainder
				if (qtyToDeliver.signum() != 0)
				{
					MMovementLineMA ma = new MMovementLineMA (line, 
						0, qtyToDeliver);
					if (!ma.save())
						;
					log.fine("##: " + ma);
				}*/
				for (MStorage storage: storages)
				{
					//consume ASI Zero
					if (storage.getM_AttributeSetInstance_ID() == 0)
					{
						qtyASI = qtyASI.add(storage.getQtyOnHand());
						qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
						continue;
					}
					
					if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
					{
						MMovementLineMA ma = new MMovementLineMA (line, 
								storage.getM_AttributeSetInstance_ID(),
								qtyToDeliver);
							if (!ma.save())
							{
								throw new IllegalStateException("Error try create ASI Reservation");
							}		
							qtyToDeliver = Env.ZERO;
							log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
							//return;
					}
					else
					{	
						MMovementLineMA ma = new MMovementLineMA (line, 
									storage.getM_AttributeSetInstance_ID(),
									storage.getQtyOnHand());
								if (!ma.save())
								{
									throw new IllegalStateException("Error try create ASI Reservation");
								}	
							qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
							log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
					}
				}
								
				//	No AttributeSetInstance found for remainder
				if (qtyToDeliver.signum() != 0 || qtyASI.signum() != 0)
				{
					MMovementLineMA ma = new MMovementLineMA (line, 0 , qtyToDeliver.add(qtyASI));
					
					if (!ma.save())
						;
					log.fine("##: " + ma);
				}
			}	//	attributeSetInstance
			
			if (needSave && !line.save())
				log.severe("NOT saved " + line);
		//}	//	for all lines

	}	//	checkMaterialPolicy

	/**
	 * 	Void Document.
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{
		log.info(toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		if (DOCSTATUS_Closed.equals(getDocStatus())
			|| DOCSTATUS_Reversed.equals(getDocStatus())
			|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			return false;
		}

		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
		{
			//	Set lines to 0
			MMovementLine[] lines = getLines(false);
			for (int i = 0; i < lines.length; i++)
			{
				MMovementLine line = lines[i];
				BigDecimal old = line.getMovementQty();
				if (old.compareTo(Env.ZERO) != 0)
				{
					line.setMovementQty(Env.ZERO);
					line.addDescription("Void (" + old + ")");
					line.save(get_TrxName());
				}
			}
		}
		else
		{
			return reverseCorrectIt();
		}
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
			
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info(toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;

		//	Close Not delivered Qty
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return false 
	 */
	public boolean reverseCorrectIt()
	{
		log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (!MPeriod.isOpen(getCtx(), getMovementDate(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return false;
		}

		//	Deep Copy
		MMovement reversal = new MMovement(getCtx(), 0, get_TrxName());
		copyValues(this, reversal, getAD_Client_ID(), getAD_Org_ID());
		reversal.setDocStatus(DOCSTATUS_Drafted);
		reversal.setDocAction(DOCACTION_Complete);
		reversal.setIsApproved (false);
		reversal.setIsInTransit (false);
		reversal.setPosted(false);
		reversal.setProcessed(false);
		reversal.addDescription("{->" + getDocumentNo() + ")");
		//FR [ 1948157  ]
		reversal.setReversal_ID(getM_Movement_ID());
		if (!reversal.save())
		{
			m_processMsg = "Could not create Movement Reversal";
			return false;
		}
		reversal.setReversal(true);
		//	Reverse Line Qty
		MMovementLine[] oLines = getLines(true);
		for (int i = 0; i < oLines.length; i++)
		{
			MMovementLine oLine = oLines[i];
			MMovementLine rLine = new MMovementLine(getCtx(), 0, get_TrxName());
			copyValues(oLine, rLine, oLine.getAD_Client_ID(), oLine.getAD_Org_ID());
			rLine.setM_Movement_ID(reversal.getM_Movement_ID());
			//AZ Goodwill			
			// store original (voided/reversed) document line
			rLine.setReversalLine_ID(oLine.getM_MovementLine_ID());
			//
			rLine.setMovementQty(rLine.getMovementQty().negate());
			rLine.setTargetQty(Env.ZERO);
			rLine.setScrappedQty(Env.ZERO);
			rLine.setConfirmedQty(Env.ZERO);
			rLine.setProcessed(false);
			if (!rLine.save())
			{
				m_processMsg = "Could not create Movement Reversal Line";
				return false;
			}
		}
		//
		if (!reversal.processIt(DocAction.ACTION_Complete))
		{
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return false;
		}
		reversal.closeIt();
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.save();
		m_processMsg = reversal.getDocumentNo();
		
		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		//	Update Reversed (this)
		addDescription("(" + reversal.getDocumentNo() + "<-)");
		//FR [ 1948157  ]
		setReversal_ID(reversal.getM_Movement_ID());
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);	//	may come from void
		setDocAction(DOCACTION_None);
		
		return true;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return false 
	 */
	public boolean reverseAccrualIt()
	{
		log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate
	 * 	@return false 
	 */
	public boolean reActivateIt()
	{
		log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;	
		
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reActivateIt
	
	
	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
		//	: Total Lines = 123.00 (#1)
		sb.append(": ")
			.append(Msg.translate(getCtx(),"ApprovalAmt")).append("=").append(getApprovalAmt())
			.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	public int getC_Currency_ID()
	{
	//	MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID());
	//	return pl.getC_Currency_ID();
		return 0;
	}	//	getC_Currency_ID
	
	/**
	 * 	Order Constructor - create header only
	 *	@param order order
	 *	@param movementDate optional movement date (default today)
	 *	@param C_DocType_ID document type or 0
	 */
	public MMovement (MDDOrder order, int C_DocType_ID, Timestamp movementDate)
	{
		this (order.getCtx(), 0, order.get_TrxName());
		setClientOrg(order);
		setC_BPartner_ID (order.getC_BPartner_ID());
		setC_BPartner_Location_ID (order.getC_BPartner_Location_ID());	//	shipment address
		setAD_User_ID(order.getAD_User_ID());
		//
		//setM_Warehouse_ID (order.getM_Warehouse_ID());
		//setIsSOTrx (order.isSOTrx());
		//setMovementType (order.isSOTrx() ? MOVEMENTTYPE_CustomerShipment : MOVEMENTTYPE_VendorReceipts);
		//if (C_DocType_ID == 0)
		//	C_DocType_ID = DB.getSQLValue(null,
		//		"SELECT C_DocType_ID FROM C_DocType WHERE C_DocType_ID=?", 
		//		order.getC_DocType_ID());
		//setC_DocType_ID (C_DocType_ID);
		
		//	Default - Today
		if (movementDate != null)
			setMovementDate (movementDate);

		//setDateAcct (getMovementDate());
		
		//	Copy from Order
		setDD_Order_ID(order.getC_Order_ID());
		setDeliveryRule (order.getDeliveryRule());
		setDeliveryViaRule (order.getDeliveryViaRule());
		setM_Shipper_ID(order.getM_Shipper_ID());
		setFreightCostRule (order.getFreightCostRule());
		setFreightAmt(order.getFreightAmt());
		setSalesRep_ID(order.getSalesRep_ID());
		//
		setC_Activity_ID(order.getC_Activity_ID());
		setC_Campaign_ID(order.getC_Campaign_ID());
		setC_Charge_ID(order.getC_Charge_ID());
		setChargeAmt(order.getChargeAmt());
		//
		setC_Project_ID(order.getC_Project_ID());
		//setDateOrdered(order.getDateOrdered());
		setDescription(order.getDescription());
		//setPOReference(order.getPOReference());		
		setSalesRep_ID(order.getSalesRep_ID());
		setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		setUser1_ID(order.getUser1_ID());
		setUser2_ID(order.getUser2_ID());
		setPriorityRule(order.getPriorityRule());
	}	//	MMovement
	
	/** Reversal Flag		*/
	private boolean m_reversal = false;
	
	/**
	 * 	Set Reversal
	 *	@param reversal reversal
	 */
	private void setReversal(boolean reversal)
	{
		m_reversal = reversal;
	}	//	setReversal
	/**
	 * 	Is Reversal
	 *	@return reversal
	 */
	private boolean isReversal()
	{
		return m_reversal;
	}	//	isReversal
	
}	//	MMovement

