/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestParamMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueString;
	private MethodParameter paramNamedStringArray;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMultipartFile;
	private MethodParameter paramMultipartFileList;
	private MethodParameter paramMultipartFileArray;
	private MethodParameter paramPart;
	private MethodParameter paramPartList;
	private MethodParameter paramPartArray;
	private MethodParameter paramMap;
	private MethodParameter paramStringNotAnnot;
	private MethodParameter paramMultipartFileNotAnnot;
	private MethodParameter paramMultipartFileListNotAnnot;
	private MethodParameter paramPartNotAnnot;
	private MethodParameter paramRequestPartAnnot;
	private MethodParameter paramRequired;
	private MethodParameter paramNotRequired;
	private MethodParameter paramStringList;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private WebDataBinderFactory binderFactory;
	
	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMethodArgumentResolver(null, true);

		ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

		Method method = getClass().getMethod("params", String.class, String[].class,
				Map.class, MultipartFile.class, List.class, MultipartFile[].class,
				Part.class, List.class, Part[].class, Map.class,
				String.class, MultipartFile.class, List.class, Part.class,
				MultipartFile.class, String.class, String.class, List.class);

		paramNamedDefaultValueString = new MethodParameter(method, 0);
		paramNamedStringArray = new MethodParameter(method, 1);
		paramNamedMap = new MethodParameter(method, 2);
		paramMultipartFile = new MethodParameter(method, 3);
		paramMultipartFileList = new MethodParameter(method, 4);
		paramMultipartFileArray = new MethodParameter(method, 5);
		paramPart = new MethodParameter(method, 6);
		paramPartList  = new MethodParameter(method, 7);
		paramPartArray  = new MethodParameter(method, 8);
		paramMap = new MethodParameter(method, 9);
		paramStringNotAnnot = new MethodParameter(method, 10);
		paramStringNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramMultipartFileNotAnnot = new MethodParameter(method, 11);
		paramMultipartFileNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramMultipartFileListNotAnnot = new MethodParameter(method, 12);
		paramMultipartFileListNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramPartNotAnnot = new MethodParameter(method, 13);
		paramPartNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramRequestPartAnnot = new MethodParameter(method, 14);
		paramRequired = new MethodParameter(method, 15);
		paramNotRequired = new MethodParameter(method, 16);
		paramStringList = new MethodParameter(method, 17);
		
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
		
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		binderFactory = new InitBinderDataBinderFactory(null, initializer);
	}

	@Test
	public void supportsParameter() {
		resolver = new RequestParamMethodArgumentResolver(null, true);
		assertTrue("String parameter not supported", resolver.supportsParameter(paramNamedDefaultValueString));
		assertTrue("String array parameter not supported", resolver.supportsParameter(paramNamedStringArray));
		assertTrue("Named map not parameter supported", resolver.supportsParameter(paramNamedMap));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(paramMultipartFile));
		assertTrue("List<MultipartFile> parameter not supported", resolver.supportsParameter(paramMultipartFileList));
		assertTrue("MultipartFile[] parameter not supported", resolver.supportsParameter(paramMultipartFileArray));
		assertTrue("Part parameter not supported", resolver.supportsParameter(paramPart));
		assertTrue("List<Part> parameter not supported", resolver.supportsParameter(paramPartList));
		assertTrue("Part[] parameter not supported", resolver.supportsParameter(paramPartArray));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(paramMap));
		assertTrue("Simple type params supported w/o annotations", resolver.supportsParameter(paramStringNotAnnot));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(paramMultipartFileNotAnnot));
		assertTrue("Part parameter not supported", resolver.supportsParameter(paramPartNotAnnot));
		assertTrue("List<String> parameter not supported", resolver.supportsParameter(paramStringList));		

		resolver = new RequestParamMethodArgumentResolver(null, false);
		assertFalse(resolver.supportsParameter(paramStringNotAnnot));
		assertFalse(resolver.supportsParameter(paramRequestPartAnnot));
	}

	@Test
	public void resolveString() throws Exception {
		String expected = "foo";
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArray() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);

		assertTrue(result instanceof String[]);
		assertArrayEquals("Invalid result", expected, (String[]) result);
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFile, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileList, null, webRequest, null);

		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test
	public void resolveMultipartFileArray() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilearray", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilearray", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileArray, null, webRequest, null);

		assertTrue(result instanceof MultipartFile[]);
		MultipartFile[] parts = (MultipartFile[]) result;
		assertEquals(parts[0], expected1);
		assertEquals(parts[1], expected2);
	}

	@Test
	public void resolvePart() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected = new MockPart("pfile", "Hello World".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPart, null, webRequest, null);

		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolvePartList() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected1 = new MockPart("pfilelist", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilelist", "Hello World 2".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected1);
		request.addPart(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartList, null, webRequest, null);

		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test
	public void resolvePartArray() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected1 = new MockPart("pfilearray", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilearray", "Hello World 2".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected1);
		request.addPart(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartArray, null, webRequest, null);

		assertTrue(result instanceof Part[]);
		Part[] parts = (Part[]) result;
		assertEquals(parts[0], expected1);
		assertEquals(parts[1], expected2);
	}

	@Test
	public void resolveMultipartFileNotAnnot() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileNotAnnot, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileListNotAnnotated() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileListNotAnnot, null, webRequest, null);

		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test(expected = MultipartException.class)
	public void isMultipartRequest() throws Exception {
		resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		fail("Expected exception: request is not a multipart request");
	}

	// SPR-9079

	@Test
	public void isMultipartRequestHttpPut() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
		request.addFile(expected);
		request.setMethod("PUT");
		webRequest = new ServletWebRequest(request);

		Object actual = resolver.resolveArgument(paramMultipartFileListNotAnnot, null, webRequest, null);

		assertTrue(actual instanceof List);
		assertEquals(expected, ((List<?>) actual).get(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingMultipartFile() throws Exception {
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		fail("Expected exception: request is not MultiPartHttpServletRequest but param is MultipartFile");
	}

	@Test
	public void resolvePartNotAnnot() throws Exception {
		MockPart expected = new MockPart("part", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartNotAnnot, null, webRequest, null);

		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = MissingServletRequestParameterException.class)
	public void missingRequestParam() throws Exception {
		resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);
		fail("Expected exception");
	}

	// SPR-10578

	@Test
	public void missingRequestParamEmptyValueConvertedToNull() throws Exception {

		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, null, "stringNotAnnot")).willReturn(binder);

		this.request.addParameter("stringNotAnnot", "");

		Object arg = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, binderFactory);

		assertNull(arg);
	}

	@Test
	public void missingRequestParamEmptyValueNotRequired() throws Exception {

		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, null, "name")).willReturn(binder);

		this.request.addParameter("name", "");

		Object arg = resolver.resolveArgument(paramNotRequired, null, webRequest, binderFactory);

		assertNull(arg);
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		request.setParameter("stringNotAnnot", "plainValue");
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("plainValue", result);
	}

	// SPR-8561

	@Test
	public void resolveSimpleTypeParamToNull() throws Exception {
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);
		assertNull(result);
	}

	// SPR-10180

	@Test
	public void resolveEmptyValueToDefault() throws Exception {
		this.request.addParameter("name", "");
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);
		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		this.request.addParameter("stringNotAnnot", "");
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);
		assertEquals("", result);
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		this.request.addParameter("name", "");
		Object result = resolver.resolveArgument(paramRequired, null, webRequest, null);
		assertEquals("", result);
	}

	
	@SuppressWarnings("unchecked")
	@Test
	public void resolveStringList() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		request.addParameter("name", expected);
		Object result = resolver.resolveArgument(paramStringList, null, webRequest, binderFactory);
		assertTrue(result instanceof List<?>);
		assertArrayEquals("Invalid result", expected, ((List<String>)result).toArray(new String[expected.length]));
	}
	
	// SPR-11126
	@SuppressWarnings("unchecked")
	@Test
	public void resolveStringListWithEmptyStringParam() throws Exception {
		String[] expected = new String[]{""};
		request.addParameter("name", expected);
		Object result = resolver.resolveArgument(paramStringList, null, webRequest, binderFactory);
		assertTrue(result instanceof List<?>);
		assertEquals(expected.length, ((List<String>)result).size());
		assertArrayEquals("Invalid result", expected, ((List<String>)result).toArray(new String[expected.length]));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void resolveStringListEmptyStringParams() throws Exception {
		String[] expected = new String[]{"", ""};
		request.addParameter("name", expected);
		Object result = resolver.resolveArgument(paramStringList, null, webRequest, binderFactory);
		assertTrue(result instanceof List<?>);
		assertEquals(expected.length, ((List<String>)result).size());
		assertArrayEquals("Invalid result", expected, ((List<String>)result).toArray(new String[expected.length]));
	}	

	public void params(@RequestParam(value = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam(value = "mfile") MultipartFile param4,
			@RequestParam(value = "mfilelist") List<MultipartFile> param5,
			@RequestParam(value = "mfilearray") MultipartFile[] param6,
			@RequestParam(value = "pfile") Part param7,
			@RequestParam(value = "pfilelist") List<Part> param8,
			@RequestParam(value = "pfilearray") Part[] param9,
			@RequestParam Map<?, ?> param10,
			String stringNotAnnot,
			MultipartFile multipartFileNotAnnot,
			List<MultipartFile> multipartFileList,
			Part part,
			@RequestPart MultipartFile requestPartAnnot,
			@RequestParam(value = "name") String paramRequired,
			@RequestParam(value = "name", required=false) String paramNotRequired,
			@RequestParam(value = "name") List<String> paramStringList) {
	}

}
