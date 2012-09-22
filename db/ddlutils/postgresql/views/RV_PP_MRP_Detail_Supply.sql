DROP VIEW RV_PP_MRP_Detail_Supply;
CREATE VIEW RV_PP_MRP_Detail_Supply AS
SELECT
mrp_detail.AD_Client_ID,
mrp_detail.AD_Org_ID,
mrp_detail.Created,
mrp_detail.CreatedBy,
mrp_detail.Updated,
mrp_detail.UpdatedBy,
mrp_detail.isActive,
mrp_detail.MRP_Demand_ID,
mrp_detail.MRP_Supply_ID,
mrp_detail.Qty,
supply.OrderType,
supply.DocStatus,
supply.DateOrdered,
supply.DatePromised,
supply.Priority,
supply.S_Resource_ID,
supply.M_Warehouse_ID,
supply.C_BPartner_ID,
supply.Planner_ID
FROM PP_MRP_Detail mrp_detail  
LEFT JOIN RV_PP_MRP supply ON (supply.PP_MRP_ID=mrp_detail.MRP_Supply_ID);

DROP VIEW RV_PP_MRP_Detail_Demand;
CREATE VIEW RV_PP_MRP_Detail_Demand AS
SELECT
mrp_detail.AD_Client_ID,
mrp_detail.AD_Org_ID,
mrp_detail.Created,
mrp_detail.CreatedBy,
mrp_detail.Updated,
mrp_detail.UpdatedBy,
mrp_detail.isActive,
mrp_detail.MRP_Demand_ID,
mrp_detail.MRP_Supply_ID,
mrp_detail.Qty,
demand.OrderType,
demand.DocStatus,
demand.DateOrdered,
demand.DatePromised,
demand.Priority,
demand.S_Resource_ID,
demand.M_Warehouse_ID,
demand.C_BPartner_ID,
demand.Planner_ID
FROM PP_MRP_Detail mrp_detail
LEFT JOIN RV_PP_MRP demand ON (demand.PP_MRP_ID=mrp_detail.MRP_demand_ID);

--DELETE FROM PP_MRP_Detail
