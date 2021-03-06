/*
 * Copyright (c) 2018 Ahome' Innovation Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ait.lienzo.client.core.shape.json.validators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class ObjectValidator extends AbstractAttributeTypeValidator
{
    private final List<String>                         m_requiredAttributes = new ArrayList<>();

    private final Map<String, IAttributeTypeValidator> m_attributes         = new LinkedHashMap<>();

    public ObjectValidator(final String typeName)
    {
        super(typeName);
    }

    public void addAttribute(final String attrName, final IAttributeTypeValidator type, final boolean required)
    {
        m_attributes.put(attrName, type);

        if (required)
        {
            m_requiredAttributes.add(attrName);
        }
    }

    @Override
    public void validate(final JSONValue jval, final ValidationContext ctx) throws ValidationException
    {
        if (null == jval)
        {
            ctx.addBadTypeError(getTypeName());

            return;
        }
        final JSONObject jobj = jval.isObject();

        if (null == jobj)
        {
            ctx.addBadTypeError(getTypeName());
        }
        else
        {
            final Set<String> keys = jobj.keySet();

            // Check required attributes

            for (final String attrName : m_requiredAttributes)
            {
                ctx.push(attrName);

                if (false == keys.contains(attrName))
                {
                    ctx.addRequiredError();// value is missing
                }
                else
                {
                    final JSONValue aval = jobj.get(attrName);

                    if ((null == aval) || (null != aval.isNull()))
                    {
                        ctx.addRequiredError();// value is null
                    }
                }
                ctx.pop();// attrName
            }
            // Now check the attribute values

            for (final String attrName : keys)
            {
                ctx.push(attrName);

                final IAttributeTypeValidator validator = m_attributes.get(attrName);

                if (null == validator)
                {
                    ctx.addInvalidAttributeError(getTypeName());
                }
                else if (false == validator.isIgnored())
                {
                    validator.validate(jobj.get(attrName), ctx);
                }
                ctx.pop();// attrName
            }
        }
    }

    protected void checkHardcodedAttribute(final String attrName, final String requiredAttrValue, final JSONValue jval, final ValidationContext ctx) throws ValidationException
    {
        // ASSUMPTION: requiredness was already checked and reported on

        final JSONObject jobj = jval.isObject();

        if (null != jobj)
        {
            final JSONValue aval = jobj.get(attrName);

            if (null != aval)
            {
                final JSONString sval = aval.isString();

                if ((null == sval) || (false == requiredAttrValue.equals(sval.stringValue())))
                {
                    ctx.push(attrName);

                    ctx.addRequiredAttributeValueError(requiredAttrValue);

                    ctx.pop();// attrName
                }
            }
        }
    }
}
