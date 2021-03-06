/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.ejb;

/**
 * <a href="AddressManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.66 $
 *
 */
public class AddressManagerUtil {
	public static com.liferay.portal.model.Address addAddress(
		java.lang.String userId, java.lang.String className,
		java.lang.String classPK, java.lang.String description,
		java.lang.String street1, java.lang.String street2,
		java.lang.String city, java.lang.String state, java.lang.String zip,
		java.lang.String country, java.lang.String phone, java.lang.String fax,
		java.lang.String cell)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.addAddress(userId, className, classPK,
				description, street1, street2, city, state, zip, country,
				phone, fax, cell);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void deleteAddress(java.lang.String addressId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();
			addressManager.deleteAddress(addressId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Address getAddress(
		java.lang.String addressId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.getAddress(addressId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List getAddresses(java.lang.String className,
		java.lang.String classPK)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.getAddresses(className, classPK);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Address getPrimaryAddress(
		java.lang.String className, java.lang.String classPK)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.getPrimaryAddress(className, classPK);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Address updateAddress(
		java.lang.String addressId, java.lang.String description,
		java.lang.String street1, java.lang.String street2,
		java.lang.String city, java.lang.String state, java.lang.String zip,
		java.lang.String country, java.lang.String phone, java.lang.String fax,
		java.lang.String cell)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.updateAddress(addressId, description,
				street1, street2, city, state, zip, country, phone, fax, cell);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void updateAddresses(java.lang.String className,
		java.lang.String classPK, java.lang.String[] addressIds)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();
			addressManager.updateAddresses(className, classPK, addressIds);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void updateAddressPriority(java.lang.String className,
		java.lang.String classPK, java.lang.String addressId, boolean priority)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();
			addressManager.updateAddressPriority(className, classPK, addressId,
				priority);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static boolean hasWrite(java.lang.String addressId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AddressManager addressManager = AddressManagerFactory.getManager();

			return addressManager.hasWrite(addressId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
}