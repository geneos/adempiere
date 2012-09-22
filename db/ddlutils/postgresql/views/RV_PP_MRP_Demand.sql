﻿DROP VIEW RV_PP_MRP_Demand;
CREATE VIEW RV_PP_MRP_Demand AS 
SELECT
mrp.AD_Client_ID,
mrp.AD_Org_ID,
mrp.Created,
mrp.CreatedBy,
mrp.Updated,
mrp.UpdatedBy,
mrp.isActive,
mrp.PP_MRP_ID, 
mrp.DocumentNo , 
mrp.OrderType , 
mrp.DocStatus,
mrp.C_Bpartner_ID,
mrp.Planner_ID , 
mrp.S_Resource_ID , 
mrp.M_Warehouse_ID , 
mrp.DateOrdered,
mrp.DatePromised , 
mrp.Priority , 
mrp.M_Product_ID , 
p.sku , p.C_UOM_ID , p.IsSold,
mrp.M_Product_Category_ID, 
mrp.isBOM, mrp.IsPurchased , 
mrp.qty,
mrp.isMPS,
mrp.isRequiredMRP,
mrp.IsRequiredDRP
FROM RV_PP_MRP mrp INNER JOIN M_Product p ON (p.M_Product_ID=mrp.M_Product_ID)
WHERE mrp.TypeMRP='D' AND mrp.Qty > 0 ORDER BY mrp.DatePromised;


DROP VIEW RV_PP_MRP_Supply;
CREATE VIEW RV_PP_MRP_Supply AS 
SELECT
mrp.AD_Client_ID,
mrp.AD_Org_ID,
mrp.Created,
mrp.CreatedBy,
mrp.Updated,
mrp.UpdatedBy,
mrp.isActive,
mrp.PP_MRP_ID, 
mrp.DocumentNo,
mrp.OrderType, 
mrp.DocStatus,
mrp.C_Bpartner_ID,
mrp.Planner_ID , 
mrp.S_Resource_ID , 
mrp.M_Warehouse_ID , 
mrp.DateOrdered,
mrp.DatePromised , 
mrp.Priority, 
mrp.M_Product_ID, 
p.sku , p.C_UOM_ID , p.IsSold,
mrp.M_Product_Category_ID, 
mrp.isBOM, mrp.IsPurchased, 
mrp.qty,
mrp.isMPS,
mrp.isRequiredMRP,
mrp.IsRequiredDRP
FROM RV_PP_MRP mrp INNER JOIN M_Product p ON (p.M_Product_ID=mrp.M_Product_ID)
WHERE mrp.TypeMRP='S' AND mrp.Qty > 0 ORDER BY mrp.DatePromised;