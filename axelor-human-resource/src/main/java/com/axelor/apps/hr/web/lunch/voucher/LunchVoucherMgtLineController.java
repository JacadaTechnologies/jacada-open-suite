/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.web.lunch.voucher;

import javax.inject.Inject;

import com.axelor.apps.hr.db.LunchVoucherMgtLine;
import com.axelor.apps.hr.service.lunch.voucher.LunchVoucherMgtLineService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class LunchVoucherMgtLineController {
	
	@Inject
	private LunchVoucherMgtLineService service;
	
	public void compute(ActionRequest request, ActionResponse response)  {
		
		try {
			LunchVoucherMgtLine line = request.getContext().asType(LunchVoucherMgtLine.class);
			service.compute(line);
			
			response.setValue("lunchVoucherNumber", line.getLunchVoucherNumber());
		}  catch(Exception e)  {
			TraceBackService.trace(response, e);
		}
	}
}
