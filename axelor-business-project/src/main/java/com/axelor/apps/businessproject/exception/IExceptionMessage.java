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
package com.axelor.apps.businessproject.exception;

/**
 * Interface of Exceptions. Enum all exception of axelor-account.
 *
 * @author dubaux
 *
 */
public interface IExceptionMessage {

	static final String FOLDER_TEMPLATE = /*$$(*/ "You must add a sale order template"/*)*/ ;
	static final String INVOICING_PROJECT_EMPTY = /*$$(*/ "You haven't select any element to invoice"/*)*/ ;
	static final String INVOICING_PROJECT_USER = /*$$(*/ "The project/task selected doesn't have any responsible"/*)*/ ;
	static final String INVOICING_PROJECT_PROJECT = /*$$(*/ "Veuillez sélectionner un(e) projet/tâche"/*)*/ ;
	static final String INVOICING_PROJECT_PROJECT_PARTNER = /*$$(*/ "There is no customer for this project/task"/*)*/ ;
	static final String INVOICING_PROJECT_PROJECT_PRODUCT = /*$$(*/ "Vous n'avez pas sélectionné de produit de facturation pour la tâche %s"/*)*/ ;
	static final String INVOICING_PROJECT_PROJECT_COMPANY = /*$$(*/ "Vous n'avez pas sélectionné de compagnie sur le projet racine"/*)*/ ;
	static final String SALE_ORDER_NO_PROJECT = /*$$(*/ "No Project selected"/*)*/ ;
	static final String SALE_ORDER_NO_LINES = /*$$(*/ "No Line can be used for tasks"/*)*/ ;
	static final String INVOICING_PROJECT_GENERATION = /*$$(*/ "Invoicing project generated"/*)*/ ;
}