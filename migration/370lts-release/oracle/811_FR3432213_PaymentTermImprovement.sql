-- C_PaySchedule

ALTER TABLE C_PaySchedule
 ADD (IsDueFixed  CHAR(1 BYTE) DEFAULT 'N' NOT NULL);
 
ALTER TABLE C_PaySchedule
 ADD (FixMonthOffset  NUMBER(10,0));
 
------------------------------------------------------------------------------------------------------------------
 
create or replace
FUNCTION               paymentTermDueDate
(
    PaymentTerm_ID    IN    NUMBER,
    DocDate            IN    DATE
)
RETURN DATE
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *************************************************************************
 * $Id: C_PaymentTerm_DueDate.sql,v 1.1 2006/04/21 17:51:58 jjanke Exp $
 ***
 * Title:    Get Due Date
 * Description:
 *    Returns the due date
 ************************************************************************
 * FreePath 2011/10/31 correction for Last Month Day
 ************************************************************************/
AS
    DueDate             DATE := TRUNC(DocDate);
	FixMonthOffset      C_PaymentTerm.FixMonthOffset%TYPE;
	MonthDay			NUMBER;
	LastMonthDay		NUMBER;
    --
    CURSOR Cur_PT    IS
        SELECT    *
        FROM    C_PaymentTerm
        WHERE    C_PaymentTerm_ID = PaymentTerm_ID;
    FirstDay            DATE;
    NoDays              NUMBER;
BEGIN

	IF PaymentTerm_ID = 0 OR DocDate IS NULL THEN
        RETURN null;
    END IF;

    FOR p IN Cur_PT LOOP    --    for convineance only
    --    DBMS_OUTPUT.PUT_LINE(p.Name || ' - Doc = ' || TO_CHAR(DocDate));
        --    Due 15th of following month
        IF (p.IsDueFixed = 'Y') THEN
        --    DBMS_OUTPUT.PUT_LINE(p.Name || ' - Day = ' || p.FixMonthDay);
            FirstDay := TRUNC(DocDate, 'MM');
            NoDays := extract (day from TRUNC(DocDate));
			
			FixMonthOffset := p.FixMonthOffset;
			IF NoDays > p.FixMonthCutoff THEN
				FixMonthOffset := FixMonthOffset + 1;
				
			END IF;
			
			DueDate := ADD_MONTHS(FirstDay, FixMonthOffset);
			
			LastMonthDay := extract (day from last_day(DueDate));
			MonthDay := p.FixMonthDay;
			
			IF p.FixMonthDay > LastMonthDay THEN
			
				MonthDay := LastMonthDay;
				
			END IF;
			
            DueDate := DueDate + (MonthDay-1);    --    starting on 1st
			
        ELSE
        --    DBMS_OUTPUT.PUT_LINE('Net = ' || p.NetDays);
            DueDate := TRUNC(DocDate) + p.NetDays;
        END IF;
    END LOOP;
--    DBMS_OUTPUT.PUT_LINE('Due = ' || TO_CHAR(DueDate) || ', Pay = ' || TO_CHAR(PayDate));

    RETURN DueDate;
END paymentTermDueDate; 

------------------------------------------------------------------------------------------------------------------

create or replace
FUNCTION             paymentTermDueDays
(
 PaymentTerm_ID IN NUMBER,
 DocDate   IN DATE,
 PayDate   IN DATE
)
RETURN NUMBER
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *************************************************************************
 * $Id: C_PaymentTerm_DueDays.sql,v 1.1 2006/04/21 17:51:58 jjanke Exp $
 ***
 * Title: Get Due Days
 * Description:
 * Returns the days due (positive) or the days till due (negative)
 * Grace days are not considered!
 * If record is not found it assumes due immediately
 *
 * Test: SELECT paymentTermDueDays(103, '01-DEC-2000', '15-DEC-2000') FROM DUAL
 *
 * Contributor(s): Carlos Ruiz - globalqss - match with SQLJ version
 *				   FreePath 2011/10/31 use of paymentTermDueDate function
 ************************************************************************/
AS
 Days    NUMBER := 0;
 DueDate    DATE := NULL;
 v_PayDate   DATE;
BEGIN

 IF PaymentTerm_ID = 0 OR DocDate IS NULL THEN
     RETURN 0;
 END IF;

 v_PayDate := PayDate;
 IF v_PayDate IS NULL THEN
     v_PayDate := TRUNC(SYSDATE);
 END IF;

 DueDate := paymentTermDueDate(PaymentTerm_ID, DocDate);
-- DBMS_OUTPUT.PUT_LINE('Due = ' || TO_CHAR(DueDate) || ', Pay = ' || TO_CHAR(v_PayDate));

 IF DueDate IS NULL THEN
     RETURN 0;
 END IF;

 Days := TRUNC(v_PayDate) - DueDate;
 RETURN Days;
END paymentTermDueDays; 