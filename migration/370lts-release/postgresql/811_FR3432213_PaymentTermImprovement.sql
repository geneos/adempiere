-- C_PaySchedule

ALTER TABLE C_PaySchedule
 ADD IsDueFixed  CHARACTER(1) DEFAULT 'N' NOT NULL;
 
ALTER TABLE C_PaySchedule
 ADD FixMonthOffset  NUMERIC(10,0);
 
-- Function: paymenttermduedate(numeric, timestamp with time zone)

-- DROP FUNCTION paymenttermduedate(numeric, timestamp with time zone);

CREATE OR REPLACE FUNCTION paymenttermduedate(paymentterm_id numeric, docdate timestamp with time zone)
  RETURNS timestamp with time zone AS
$BODY$
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *
 * converted to postgreSQL by Karsten Thiemann (Schaeffer AG), 
 * kthiemann@adempiere.org
 *************************************************************************
 * Title:	Get Due timestamp with time zone
 * Description:
 *	Returns the due timestamp with time zone
 * Test:
 *	select paymenttermDueDate(106, now()) from Test; => now()+30 days
 *
 * Contributor(s): Carlos Ruiz - globalqss - match with SQLJ version
 *                 FreePath 2011/10/31 correction for Last Month Day
 ************************************************************************/
DECLARE
	DueDate			timestamp with time zone := NULL;
	FixMonthOffset  C_PaymentTerm.FixMonthOffset%TYPE;
	MonthDay		NUMERIC;
	LastMonthDay	NUMERIC;
	p   			RECORD;
	--
	FirstDay			timestamp with time zone;
	NoDays				NUMERIC;
BEGIN

    IF PaymentTerm_ID = 0 OR DocDate IS NULL THEN
	    RETURN null;
	END IF;

	FOR p IN 
		SELECT	*
		FROM	C_PaymentTerm
		WHERE	C_PaymentTerm_ID = PaymentTerm_ID
	LOOP	--	for convineance only

		--	Due 15th of following month
		IF (p.IsDueFixed = 'Y') THEN

			FirstDay := TRUNC(DocDate, 'MM');
			NoDays := extract (day from TRUNC(DocDate));
			
			FixMonthOffset := p.FixMonthOffset;
			IF NoDays > p.FixMonthCutoff THEN
			    FixMonthOffset := FixMonthOffset + 1;
				raise notice 'FixMonthOffset: %' , FixMonthOffset;
			END IF;
			
			DueDate := FirstDay + (FixMonthOffset || ' month')::interval;
			
			LastMonthDay := extract (day from (cast(date_trunc('month', DueDate) + '1 month'::interval as date) - 1));
			MonthDay := p.FixMonthDay;
			
			IF p.FixMonthDay > LastMonthDay THEN
			
				MonthDay := LastMonthDay;
				
			END IF;
			
			DueDate := DueDate + (MonthDay-1);	--	starting on 1st

		ELSE
			DueDate := TRUNC(DocDate) + p.NetDays;
		END IF;
	END LOOP;
	RETURN DueDate;
END;

$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION paymenttermduedate(numeric, timestamp with time zone)
  OWNER TO adempiere;

------------------------------------------------------------------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION paymenttermduedays(paymentterm_id numeric, docdate timestamp with time zone, paydate timestamp with time zone)
  RETURNS integer AS
$BODY$
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *
 * converted to postgreSQL by Karsten Thiemann (Schaeffer AG), 
 * kthiemann@adempiere.org
 *************************************************************************
 * Title:	Get Due Days
 * Description:
 *	Returns the days due (positive) or the days till due (negative)
 *	Grace days are not considered!
 *	If record is not found it assumes due immediately
 *
 *	Test:	SELECT paymenttermDueDays(103, now(), now());
 *
 * Contributor(s): Carlos Ruiz - globalqss - match with SQLJ version
 * 				   FreePath 2011/10/31 use of paymentTermDueDate function
 ************************************************************************/
DECLARE
 	Days			NUMERIC := 0;
	DueDate			timestamp with time zone := NULL;
	v_PayDate		timestamp with time zone;
BEGIN

    IF PaymentTerm_ID = 0 OR DocDate IS NULL THEN
	    RETURN 0;
	END IF;

    v_PayDate := PayDate;
	IF v_PayDate IS NULL THEN
	    v_PayDate := TRUNC(now());
	END IF;

	DueDate := paymentTermDueDate(PaymentTerm_ID, DocDate);

    IF DueDate IS NULL THEN
	    RETURN 0;
	END IF;


	Days := EXTRACT(day from (TRUNC(v_PayDate) - DueDate));
	RETURN Days;
END;

$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION paymenttermduedays(numeric, timestamp with time zone, timestamp with time zone)
  OWNER TO adempiere;
