package microsim.statistics.reflectors;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import microsim.reflection.ReflectionUtils;
import microsim.statistics.ILongSource;

import org.apache.log4j.Logger;

/**
 * Not of interest for users. It uses java reflection to call objects' methods
 * which return double values. It is used by Statistics objects.
 * 
 * <p>
 * Title: JAS
 * </p>
 * <p>
 * Description: Java Agent-based Simulation library
 * </p>
 * <p>
 * Copyright (C) 2002 Michele Sonnessa
 * </p>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 * 
 * @author Michele Sonnessa
 *         <p>
 */
public class LongInvoker implements ILongSource {
	private static Logger log = Logger.getLogger(LongInvoker.class);
	
	protected Method method;
	protected Field field;
	protected Object target;

	/**
	 * Constructor.
	 * 
	 * @param target
	 *            It is the target object.
	 * @param fieldName
	 *            A string representing the name of the method to invoke.
	 * @param isMethod
	 *            If true the fieldName is a method, otherwise it is a property
	 *            of the object.
	 */
	public LongInvoker(Object target, String fieldName, boolean isMethod) {
		this.target = target;
		if (isMethod)
			buildMethod(target.getClass(), fieldName);
		else
			buildField(target.getClass(), fieldName);
	}

	/**
	 * Constructor.
	 * 
	 * @param target
	 *            It is the class of the target object.
	 * @param fieldName
	 *            A string representing the name of the method to invoke.
	 * @param isMethod
	 *            If true the fieldName is a method, otherwise it is a property
	 *            of the object.
	 */
	public LongInvoker(Class<?> target, String fieldName, boolean isMethod) {
		this.target = null;
		if (isMethod)
			buildMethod(target, fieldName);
		else
			buildField(target, fieldName);
	}

	private void buildField(Class<?> trgClass, String fieldName) {
		method = null;
		field = ReflectionUtils.searchField(trgClass, fieldName);

		if (field == null)
			log.error("LongInvoker: Field " + fieldName
					+ " of object " + target + " does not exist.");

		if (field.getType() != Long.TYPE)
			log.error("LongInvoker: Field " + fieldName
					+ " of object " + target + " must return a long value!");
	}

	private void buildMethod(Class<?> trgClass, String methodName) {
		field = null;
		method = ReflectionUtils.searchMethod(trgClass, methodName);

		if (method == null)
			log.error("LongInvoker: Method " + methodName
					+ " of object " + target + " does not exist.");

		if (method.getReturnType() != Long.TYPE)
			log.error("LongInvoker: Method " + methodName
					+ " of object " + target + " must return a long value!");
	}

	/**
	 * Invoke the method of the target object and return its double result.
	 * 
	 * @param target
	 *            Object to be invoked.
	 * @return The requested double value.
	 */
	public long getLong(Object target) {
		if (target == null)
			return 0L;

		try {
			if (method == null) {
				try {
					return field.getLong(target);
				} catch (IllegalArgumentException e) {
					return ((Long) field.get(target)).longValue();
				}
			} else
				return ((Long) method.invoke(target, null)).longValue();
		} catch (InvocationTargetException ie) {
			StringBuffer message = new StringBuffer();
			if (method == null)
				message.append("LngInvoker: Field " + field + "of object "
						+ target + " raised the following error:\n");
			else
				message.append("LngInvoker: Method " + method + "of object "
						+ target + " raised the following error:\n");
			message.append(ie.getMessage());
			ie.printStackTrace();
			log.error(message.toString());

		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
			log.error("");
		}
		return 0L;
	}

	/**
	 * Invoke the method of the object passed to constructor and return its
	 * double result.
	 * 
	 * @return The requested double value.
	 */
	public long getLong() {
		return getLong(target);
	}

	/**
	 * This is an implementation of the IDblSource interface. It calls the
	 * getDouble() method.
	 * 
	 * @param valueID
	 *            This parameter is ignored. It is put for compatibility with
	 *            the IDblSource interface.
	 * @return The requested double value.
	 */
	public long getLongValue(Enum<?> valueID) {
		return getLong(target);
	}

}