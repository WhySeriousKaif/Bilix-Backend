# Pagination in Bilix POS: Complete Learning Guide

This document explains the customer pagination implementation and compares it with the same pattern used by the refund page and order history.

The important idea is simple:

1. The browser keeps the current page, page size, sorting, and search in the URL.
2. React reads those values from the URL.
3. React sends them to the backend API.
4. Spring Boot converts them into a `Pageable` object.
5. The database returns only the requested slice of rows.
6. The backend returns both the rows and pagination metadata.
7. React renders the rows and enables/disables Previous and Next buttons.

## 1. What pagination means

Without pagination, the frontend requests every customer:

```text
GET /api/customers
```

That becomes expensive when there are thousands of customers.

With pagination, the frontend requests only one page:

```text
GET /api/customers/paged?page=0&size=10&sort=createdAt,desc
```

`page=0` means the first page. Backend page numbers are zero-based.

`size=10` means return at most ten records.

`sort=createdAt,desc` means newest customers first.

## 2. URL state

The customer screen URL now looks like this:

```text
/cashier/customers?page=0&size=10&sortBy=createdAt&direction=desc
```

With a search term:

```text
/cashier/customers?page=0&size=10&sortBy=name&direction=asc&search=rahul
```

The URL is useful because refresh, browser back/forward, and shared links preserve the table state.

## 3. Backend controller

File: `backend/src/main/java/com/molla/controllers/CustomerController.java`

These imports enable Spring pagination:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
```

The paged endpoint is:

```java
@GetMapping("/paged")
public ResponseEntity<Page<Customer>> getCustomersPaged(
        @RequestParam(value = "search", required = false) String search,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
    return ResponseEntity.ok(customerService.getCustomers(search, pageable));
}
```

What happens here:

- `search` is an optional search text.
- `Pageable` receives `page`, `size`, and `sort` from the query string.
- `@PageableDefault(size = 10, sort = "createdAt")` supplies safe defaults.
- The response is a Spring `Page<Customer>`, not just a plain list.

The complete customer controller currently contains this endpoint alongside the normal CRUD endpoints:

```java
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable("id") Long id,@RequestBody Customer customer) throws Exception {
        return ResponseEntity.ok(customerService.updateCustomer(customer, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable("id") Long id) throws Exception {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(new ApiResponse("Customer deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() throws Exception {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Customer>> getCustomersPaged(
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomers(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable("id") Long id) throws Exception {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> searchCustomers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "q", required = false) String q) throws Exception {
        String searchTerm = keyword != null ? keyword : (q != null ? q : "");
        if (searchTerm.isEmpty()) {
            throw new IllegalArgumentException("Either 'keyword' or 'q' parameter is required");
        }
        return ResponseEntity.ok(customerService.searchCustomers(searchTerm));
    }
}
```

## 4. Backend service interface

File: `backend/src/main/java/com/molla/service/CustomerService.java`

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Customer createCustomer(Customer customer);
    Customer updateCustomer(Customer customer,Long id) throws Exception;
    void deleteCustomer(Long id) throws Exception;
    Customer getCustomerById(Long id) throws Exception;
    List<Customer> getAllCustomers() throws Exception;
    List<Customer> searchCustomers(String keyword) throws Exception;
    Page<Customer> getCustomers(String keyword, Pageable pageable);
}
```

The important line is:

```java
Page<Customer> getCustomers(String keyword, Pageable pageable);
```

It says that the service returns a page of customers and accepts Spring's pagination instructions.

## 5. Backend service implementation

File: `backend/src/main/java/com/molla/service/impl/CustomerServiceImpl.java`

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

```java
@Override
public Page<Customer> getCustomers(String keyword, Pageable pageable) {
    if (keyword == null || keyword.isBlank()) {
        return customerRepository.findAll(pageable);
    }

    return customerRepository
            .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    keyword.trim(),
                    keyword.trim(),
                    pageable
            );
}
```

The service chooses one of two database queries:

- No search text: fetch a page of all customers.
- Search text: fetch a page where name or email contains the text.

## 6. Backend repository

File: `backend/src/main/java/com/molla/repository/CustomerRepository.java`

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name,
            String email
    );

    Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name,
            String email,
            Pageable pageable
    );
}
```

Spring Data JPA creates the SQL behavior from the method name. The `Pageable` argument makes the database query include page limits and sorting.

## 7. Backend response shape

A Spring `Page<Customer>` looks conceptually like this:

```json
{
  "content": [
    {
      "id": 4,
      "name": "Rahul Sharma",
      "email": "rahul@example.com",
      "phone": "+919876512340"
    }
  ],
  "totalElements": 24,
  "totalPages": 3,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "numberOfElements": 10
}
```

The frontend uses:

- `content`: rows to render.
- `totalElements`: total matching customer count.
- `totalPages`: number of pages.
- `number`: current zero-based page.

## 8. Frontend API function

File: `pos-frontend/src/services/api.js`

```javascript
export const customerAPI = {
  getAll: () => apiCall('/customers'),

  getPaged: (page = 0, size = 10, sortBy = 'createdAt', direction = 'desc', search = '') => {
    const params = new URLSearchParams({
      page,
      size,
      sort: `${sortBy},${direction}`,
    })

    if (search.trim()) {
      params.set('search', search.trim())
    }

    return apiCall(`/customers/paged?${params.toString()}`)
  },

  getById: (id) => apiCall(`/customers/${id}`),
  search: (keyword) => apiCall(`/customers/search?keyword=${encodeURIComponent(keyword)}`),
  create: (customer) => apiCall('/customers', {
    method: 'POST',
    body: JSON.stringify(customer),
  }),
  update: (id, customer) => apiCall(`/customers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(customer),
  }),
  delete: (id) => apiCall(`/customers/${id}`, {
    method: 'DELETE',
  }),
}
```

For example:

```javascript
customerAPI.getPaged(1, 10, 'name', 'asc', 'rahul')
```

creates a request equivalent to:

```text
GET /api/customers/paged?page=1&size=10&sort=name,asc&search=rahul
```

## 9. Frontend URL state

File: `pos-frontend/src/pages/cashier/customers/CustomersPage.jsx`

```javascript
import { useSearchParams } from 'react-router-dom'
```

```javascript
const [searchParams, setSearchParams] = useSearchParams()
const [searchQuery, setSearchQuery] = useState(() => searchParams.get('search') || '')
const [page, setPage] = useState(() => Math.max(0, Number(searchParams.get('page') || 0)))
const pageSize = Number(searchParams.get('size') || 10)
const [sortBy, setSortBy] = useState(() => searchParams.get('sortBy') || 'createdAt')
const [direction, setDirection] = useState(() => searchParams.get('direction') || 'desc')
const [totalPages, setTotalPages] = useState(0)
const [totalElements, setTotalElements] = useState(0)
```

`useSearchParams` reads and writes the browser query string without manually using `window.location`.

## 10. Loading a page from the backend

```javascript
useEffect(() => {
  fetchCustomers()
}, [page, pageSize, sortBy, direction, searchQuery])
```

Whenever one of these values changes, a new backend request is made.

```javascript
const fetchCustomers = async () => {
  try {
    setLoading(true)
    const result = await customerAPI.getPaged(
      page,
      pageSize,
      sortBy,
      direction,
      searchQuery
    )

    const customersWithOrders = await Promise.all(
      (result?.content || []).map(async (customer) => {
        try {
          const orders = await orderAPI.getByCustomerId(customer.id)
          return {
            ...customer,
            orderCount: orders?.length || 0,
            totalSpent: orders?.reduce(
              (sum, order) => sum + (order.totalAmount || 0),
              0
            ) || 0,
          }
        } catch {
          return {
            ...customer,
            orderCount: 0,
            totalSpent: 0,
          }
        }
      })
    )

    setCustomers(customersWithOrders)
    setTotalPages(result?.totalPages || 0)
    setTotalElements(result?.totalElements || 0)
  } catch (error) {
    console.error('Error fetching customers:', error)
    setCustomers([])
  } finally {
    setLoading(false)
  }
}
```

Notice that the table uses `result.content`, not the entire response object.

## 11. Writing state back to the URL

```javascript
useEffect(() => {
  const next = new URLSearchParams(searchParams)

  next.set('page', String(page))
  next.set('size', String(pageSize))
  next.set('sortBy', sortBy)
  next.set('direction', direction)

  if (searchQuery.trim()) {
    next.set('search', searchQuery.trim())
  } else {
    next.delete('search')
  }

  setSearchParams(next, { replace: true })
}, [page, pageSize, sortBy, direction, searchQuery])
```

`replace: true` changes the current URL without creating a browser-history entry for every key press.

## 12. Resetting to the first page

```javascript
useEffect(() => {
  setPage(0)
}, [searchQuery])
```

If the user is on page 4 and starts a new search, the search results should begin at page 1. Internally page 1 is `0`.

Sorting and changing page size also reset the page:

```javascript
onValueChange={(value) => {
  const [sort, dir] = value.split(':')
  setSortBy(sort)
  setDirection(dir)
  setPage(0)
}}
```

```javascript
onValueChange={(value) => {
  setPage(0)
  const next = new URLSearchParams(searchParams)
  next.set('size', value)
  setSearchParams(next, { replace: true })
}}
```

## 13. Page-size and sort controls

```jsx
<Select
  value={String(pageSize)}
  onValueChange={(value) => {
    setPage(0)
    const next = new URLSearchParams(searchParams)
    next.set('size', value)
    setSearchParams(next, { replace: true })
  }}
>
  <SelectTrigger className="w-32">
    <SelectValue />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="10">10 / page</SelectItem>
    <SelectItem value="25">25 / page</SelectItem>
    <SelectItem value="50">50 / page</SelectItem>
  </SelectContent>
</Select>
```

```jsx
<Select
  value={`${sortBy}:${direction}`}
  onValueChange={(value) => {
    const [sort, dir] = value.split(':')
    setSortBy(sort)
    setDirection(dir)
    setPage(0)
  }}
>
  <SelectTrigger className="w-44">
    <SelectValue />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="createdAt:desc">Newest first</SelectItem>
    <SelectItem value="name:asc">Name A–Z</SelectItem>
    <SelectItem value="name:desc">Name Z–A</SelectItem>
  </SelectContent>
</Select>
```

## 14. Search input

```jsx
<Input
  type="text"
  placeholder="Search by name, email, or phone..."
  value={searchQuery}
  onChange={(e) => setSearchQuery(e.target.value)}
  className="pl-10"
/>
```

The input changes React state. The state-writing effect updates the URL, and the data-loading effect calls the backend.

## 15. Rendering the page controls

```jsx
{!loading && totalPages > 0 && (
  <div className="flex items-center justify-between border-t p-4 text-sm">
    <span>
      Page {page + 1} of {totalPages} · {totalElements} customers
    </span>

    <div className="flex gap-2">
      <Button
        variant="outline"
        size="sm"
        disabled={page === 0}
        onClick={() => setPage(value => value - 1)}
      >
        Previous
      </Button>

      <Button
        variant="outline"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => setPage(value => value + 1)}
      >
        Next
      </Button>
    </div>
  </div>
)}
```

The user sees page numbers starting at 1, while the backend uses zero-based values. That is why the label uses `{page + 1}`.

## 16. The same pattern in the refund page

The refund page follows the same core structure:

```javascript
const [searchParams, setSearchParams] = useSearchParams()
const [page, setPage] = useState(() => Number(searchParams.get('page') || 0))
const pageSize = Number(searchParams.get('size') || 10)
const [sortBy, setSortBy] = useState(() => searchParams.get('sortBy') || 'createdAt')
const [direction, setDirection] = useState(() => searchParams.get('direction') || 'desc')

useEffect(() => {
  initializeRefundPage()
}, [page, pageSize, sortBy, direction])

const [result, shift] = await Promise.all([
  orderAPI.getByBranchPaged(
    profile.branchId,
    page,
    pageSize,
    sortBy,
    direction
  ),
  shiftReportAPI.getCurrent(profile.id).catch(() => null),
])

setTotalPages(result?.totalPages || 0)
setTotalElements(result?.totalElements || 0)
setOrders((result?.content || []).filter(order =>
  ['COMPLETED', 'PARTIALLY_REFUNDED'].includes(order.status)
))
```

Its URL synchronization is:

```javascript
useEffect(() => {
  const next = new URLSearchParams(searchParams)
  next.set('page', String(page))
  next.set('size', String(pageSize))
  next.set('sortBy', sortBy)
  next.set('direction', direction)

  if (searchQuery.trim()) next.set('search', searchQuery.trim())
  else next.delete('search')

  setSearchParams(next, { replace: true })
}, [page, pageSize, sortBy, direction, searchQuery])
```

## 17. The same pattern in order history

Order history also reads URL values first:

```javascript
const [urlParams, setUrlParams] = useSearchParams()
const [searchQuery, setSearchQuery] = useState(() => urlParams.get('search') || '')
const [dateFilter, setDateFilter] = useState(() => urlParams.get('range') || 'today')
const [page, setPage] = useState(() => Number(urlParams.get('page') || 0))
const pageSize = Number(urlParams.get('size') || 10)
```

It writes the state back to the URL:

```javascript
useEffect(() => {
  const next = new URLSearchParams(urlParams)
  next.set('page', String(page))
  next.set('size', String(pageSize))
  next.set('range', dateFilter)

  if (searchQuery.trim()) next.set('search', searchQuery.trim())
  else next.delete('search')

  setUrlParams(next, { replace: true })
}, [page, pageSize, dateFilter, searchQuery])
```

The customer page extends this idea by adding `sortBy` and `direction`.

## 18. Complete request flow

For this URL:

```text
/cashier/customers?page=1&size=10&sortBy=name&direction=asc&search=rahul
```

The flow is:

```text
Browser URL
    ↓
useSearchParams()
    ↓
React state: page=1, size=10, sortBy=name, direction=asc, search=rahul
    ↓
customerAPI.getPaged(...)
    ↓
GET /api/customers/paged?page=1&size=10&sort=name,asc&search=rahul
    ↓
CustomerController
    ↓
CustomerService
    ↓
CustomerRepository
    ↓
Database returns only matching rows for page 1
    ↓
Spring Page<Customer> response
    ↓
React renders response.content
```

## 19. Important concepts learned

### Client-side pagination

The browser downloads every row and then hides most rows. This is easy but does not scale.

### Server-side pagination

The browser asks the server for one page. The database does the limiting and sorting. This is the approach used here.

### `Page<T>`

`Page<Customer>` contains rows plus metadata. A plain `List<Customer>` contains only rows, so the frontend cannot reliably know how many pages exist.

### Zero-based page indexes

Backend page 0 is shown to users as page 1:

```javascript
Page {page + 1} of {totalPages}
```

### Search and pagination

Search must be part of the backend request. Otherwise the frontend would search only the currently loaded page and miss matching records on other pages.

### Sorting and pagination

Sorting belongs in the backend request so the order is stable across all pages. Sorting only the current page gives incorrect global ordering.

### URL state

The URL is a shareable representation of the table state. It makes refresh and navigation predictable.

## 20. Validation commands used

Frontend lint for the customer page:

```bash
cd pos-frontend
npx eslint src/pages/cashier/customers/CustomersPage.jsx
```

Frontend production build:

```bash
npm run build
```

Backend compile:

```bash
cd backend
mvn -q -DskipTests compile
```

The production build and backend compile pass. Existing React hook dependency warnings remain warnings, not build errors.

## 21. POS terminal product pagination

The POS terminal is different from the customer-management page. It should not download the entire product catalog when the cashier opens a transaction.

The terminal loads ten products, then loads another ten when the product panel is scrolled near the bottom. This is called infinite scroll or lazy loading.

### Product backend endpoint

The product endpoint is:

```text
GET /api/products/storeId/{storeId}/paged?page=0&size=10&sort=createdAt,desc
```

The backend repository adds a pageable query:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStoreId(Long storeId);
    Page<Product> findByStoreId(Long storeId, Pageable pageable);
}
```

The service maps each entity in the page to a DTO:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Override
public Page<ProductDto> getProductsPaged(Long storeId, Pageable pageable) {
    accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);
    return productRepository.findByStoreId(storeId, pageable).map(ProductMapper::toDto);
}
```

The service interface contains:

```java
Page<ProductDto> getProductsPaged(Long storeId, Pageable pageable);
```

The controller exposes it:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@GetMapping("/storeId/{storeId}/paged")
public ResponseEntity<Page<ProductDto>> getProductsPaged(
        @PathVariable Long storeId,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
        @RequestHeader(value = "Authorization", required = false) String jwt
) throws UserException {
    return ResponseEntity.ok(productService.getProductsPaged(storeId, pageable));
}
```

### Product frontend API

File: `pos-frontend/src/services/api.js`

```javascript
getByStoreIdPaged: (
  storeId,
  page = 0,
  size = 10,
  sortBy = 'createdAt',
  direction = 'desc'
) => apiCall(
  `/products/storeId/${storeId}/paged?page=${page}&size=${size}&sort=${sortBy},${direction}`
),
```

### Product state

File: `pos-frontend/src/pages/cashier/ProductSection/ProductSection.jsx`

```javascript
import { useSearchParams } from "react-router-dom";
```

```javascript
const [searchParams, setSearchParams] = useSearchParams();
const [products, setProducts] = useState([]);
const [loading, setLoading] = useState(false);
const [loadingMore, setLoadingMore] = useState(false);
const [page, setPage] = useState(() =>
  Number(searchParams.get('productPage') || 0)
);
const [hasMore, setHasMore] = useState(true);
```

Meaning:

- `products` contains all product pages loaded so far.
- `page` is the last loaded zero-based page.
- `loading` is for the first page.
- `loadingMore` is for later pages.
- `hasMore` prevents requests after the last backend page.

### Product loading function

```javascript
const fetchProducts = async (requestedPage = 0, append = false) => {
  try {
    if (append) setLoadingMore(true);
    else setLoading(true);

    setError(null);
    const profile = await userAPI.getProfile();

    if (!profile?.storeId) {
      setError('Your account is not assigned to a store. Contact your manager.');
      setProducts([]);
      return;
    }

    const result = await productAPI.getByStoreIdPaged(
      profile.storeId,
      requestedPage,
      10
    );

    const data = result?.content || [];

    setProducts(current =>
      append ? [...current, ...data] : data
    );

    setPage(requestedPage);
    setHasMore(!result?.last && data.length > 0);

    const next = new URLSearchParams(searchParams);
    next.set('productPage', String(requestedPage));
    next.set('productSize', '10');
    next.set('productSort', 'createdAt');
    next.set('productDirection', 'desc');
    setSearchParams(next, { replace: true });
  } catch (err) {
    console.error('Error fetching products:', err);
    setError('Failed to load products');
    if (!append) setProducts([]);
  } finally {
    setLoading(false);
    setLoadingMore(false);
  }
};
```

The key difference between replacing and appending is:

```javascript
append ? [...current, ...data] : data
```

Page 0 replaces the list. Page 1 and later pages append to the list.

### Loading the next product page

```javascript
const loadMoreProducts = () => {
  if (loading || loadingMore || !hasMore) return;
  fetchProducts(page + 1, true);
};
```

The guards prevent duplicate requests while a request is already running or when the backend says the last page has been reached.

### Product initial load and refresh

```javascript
useEffect(() => {
  fetchProducts(page, page > 0);

  const refreshProducts = () => fetchProducts(0, false);
  window.addEventListener('productsUpdated', refreshProducts);

  return () => window.removeEventListener('productsUpdated', refreshProducts);
}, []);
```

After checkout, the `productsUpdated` event refreshes stock from page 0.

### Product scroll listener

The product grid is a scroll container:

```jsx
<div
  className="flex-1 overflow-y-auto min-h-0 px-4 sm:px-6 pb-4 sm:pb-6"
  onScroll={(event) => {
    const element = event.currentTarget;

    if (
      element.scrollHeight - element.scrollTop - element.clientHeight < 160
    ) {
      loadMoreProducts();
    }
  }}
>
  {/* product cards */}
</div>
```

The calculation means “when fewer than 160 pixels remain, start loading the next page.” Starting before the exact bottom gives the user a smoother experience.

The loader is rendered below the cards:

```jsx
{loadingMore && (
  <div className="flex justify-center py-4 text-sm text-slate-400">
    Loading more products...
  </div>
)}
```

### Product cards

```jsx
{filteredProducts.map((product) => (
  <ProductCard
    key={product.id}
    product={product}
    onClick={() => addToCart(product)}
  />
))}
```

The cards use the combined list, so the first ten remain visible while the next ten are appended.

## 22. POS terminal customer-picker pagination

The customer picker uses a separate pagination state from products. It must not reuse `productPage` or `page` because the cashier can scroll products and later open the customer picker independently.

### Customer-picker URL parameters

While the customer picker is open, it uses:

```text
/cashier?customerPage=0&customerSize=10&customerSort=createdAt&customerDirection=desc
```

Product state uses different names:

```text
/cashier?productPage=2&productSize=10&productSort=createdAt&productDirection=desc
```

Both sets can exist at the same time without conflict.

### Customer-picker state

File: `pos-frontend/src/pages/cashier/CustomerPaymentSection/CustomerPaymentSection.jsx`

```javascript
const [customerParams, setCustomerParams] = useSearchParams();
const [customerPage, setCustomerPage] = useState(() =>
  Number(customerParams.get('customerPage') || 0)
);
const customerPageSize = Number(
  customerParams.get('customerSize') || 10
);
const [customerSort, setCustomerSort] = useState(() =>
  customerParams.get('customerSort') || 'createdAt'
);
const [customerDirection, setCustomerDirection] = useState(() =>
  customerParams.get('customerDirection') || 'desc'
);
const [customerHasMore, setCustomerHasMore] = useState(true);
const [customerLoadingMore, setCustomerLoadingMore] = useState(false);
```

### Loading customers

```javascript
useEffect(() => {
  if (isCustomerDialogOpen) {
    fetchCustomers();
  }
}, [
  isCustomerDialogOpen,
  customerSearch,
  customerPage,
  customerPageSize,
  customerSort,
  customerDirection,
]);
```

```javascript
const fetchCustomers = async () => {
  try {
    setLoading(true);

    const result = await customerAPI.getPaged(
      customerPage,
      customerPageSize,
      customerSort,
      customerDirection,
      customerSearch
    );

    const nextCustomers = result?.content || [];

    setCustomers(current =>
      customerPage === 0
        ? nextCustomers
        : [...current, ...nextCustomers]
    );

    setCustomerHasMore(
      !result?.last &&
      customerPage < (result?.totalPages || 1) - 1
    );
  } catch (err) {
    console.error('Error fetching customers:', err);
    setCustomers([]);
  } finally {
    setLoading(false);
    setCustomerLoadingMore(false);
  }
};
```

### Customer search/sort reset

```javascript
useEffect(() => {
  setCustomerPage(0);
  setCustomerHasMore(true);
}, [customerSearch, customerSort, customerDirection, customerPageSize]);
```

If the search or sort changes, old results are discarded and the new result starts from page 0.

### Customer-picker infinite scroll

```javascript
const loadMoreCustomers = () => {
  if (customerLoadingMore || !customerHasMore || loading) return;

  setCustomerLoadingMore(true);
  setCustomerPage(page => page + 1);
};
```

```jsx
<div
  className="max-h-[40vh] sm:max-h-60 overflow-y-auto space-y-2"
  onScroll={(event) => {
    const element = event.currentTarget;

    if (
      element.scrollHeight - element.scrollTop - element.clientHeight < 48
    ) {
      loadMoreCustomers();
    }
  }}
>
  {customers.map((customer) => (
    <div
      key={customer.id}
      className="p-2 sm:p-3 border rounded-lg cursor-pointer hover:bg-accent transition-colors"
      onClick={() => handleSelectCustomer(customer)}
    >
      <p className="font-medium text-xs sm:text-sm">{customer.name}</p>
      <p className="text-[10px] sm:text-xs text-muted-foreground truncate">
        {customer.email}
      </p>
      <p className="text-[10px] sm:text-xs text-muted-foreground">
        {customer.phone}
      </p>
    </div>
  ))}
</div>
```

The customer picker loader is:

```jsx
{customerLoadingMore && (
  <p className="text-xs text-muted-foreground text-center">
    Loading more customers…
  </p>
)}
```

## 23. Removing customer URL parameters when the picker closes

Product pagination should remain in the URL while the cashier is on the transaction screen. Customer-picker parameters are temporary, so they are removed when the picker closes or a customer is selected.

```javascript
const clearCustomerUrlParams = () => {
  const next = new URLSearchParams(customerParams);

  [
    'customerPage',
    'customerSize',
    'customerSort',
    'customerDirection',
    'customerSearch',
  ].forEach((key) => next.delete(key));

  setCustomerParams(next, { replace: true });
};
```

When a customer is selected:

```javascript
const handleSelectCustomer = (customer) => {
  setSelectedCustomer(customer);
  setIsCustomerDialogOpen(false);
  setCustomerSearch('');
  setIsAddingCustomer(false);
  setNewCustomer({ name: '', email: '', phone: '' });
  setAddCustomerError(null);
  clearCustomerUrlParams();
};
```

When the dialog is closed with the X button or outside click:

```jsx
<Dialog
  open={isCustomerDialogOpen}
  onOpenChange={(open) => {
    setIsCustomerDialogOpen(open);

    if (!open) {
      clearCustomerUrlParams();
      setIsAddingCustomer(false);
      setAddCustomerError(null);
    }
  }}
>
```

This is why the URL no longer keeps customer parameters after the modal is dismissed.

## 24. Product and customer URL example together

While the picker is open after loading the third product page and second customer page, the URL can be:

```text
/cashier?productPage=2&productSize=10&productSort=createdAt&productDirection=desc&customerPage=1&customerSize=10&customerSort=createdAt&customerDirection=desc
```

After selecting a customer, it becomes:

```text
/cashier?productPage=2&productSize=10&productSort=createdAt&productDirection=desc
```

The product position remains available, while temporary customer-picker state is removed.

## 25. Why this is optimized

- The initial transaction screen does not download the complete product catalog.
- The customer picker does not download the complete customer table.
- Each request returns only ten records.
- Product and customer requests have separate loading flags.
- The UI appends records instead of replacing the visible list.
- `last` and `hasMore` prevent unnecessary requests.
- URL parameters preserve the durable product-list state.
- Temporary customer-picker parameters are cleaned up when the picker closes.

## 26. Important current limitation

Product category counts are currently calculated from products already loaded in the browser:

```javascript
const categories = useMemo(() => {
  const counts = new Map();

  products.forEach((p) => {
    const name = p.category?.name || 'Others';
    counts.set(name, (counts.get(name) || 0) + 1);
  });

  return [
    { name: 'All', count: products.length },
    ...Array.from(counts.entries()).map(([name, count]) => ({ name, count })),
  ];
}, [products]);
```

Therefore, category counts represent loaded products rather than the entire database until the backend provides aggregate category counts. The pagination itself is server-side and still loads only ten products per request.

## 27. What is complete so far

The cashier pagination work is complete for the main flows:

- Cashier order history: URL-based page and size state.
- Cashier returns/refunds: server-side page, size, search, and sort state.
- Cashier customer management: server-side page, size, search, and sort state.
- POS product picker: server-side pages of ten products with infinite scroll.
- POS customer picker: server-side pages of ten customers with infinite scroll.
- Product and customer picker state use separate query-parameter names.
- Temporary customer-picker query parameters are removed when the picker closes.
- Loading indicators prevent the cashier from thinking the list is frozen.

## 28. Remaining pagination work

The same review still needs to be applied to the other panels. These pages should not continue loading complete database lists when their datasets can become large:

### Branch panel

- `src/pages/branch/customers/CustomersPage.jsx`
- `src/pages/branch/employees/EmployeesPage.jsx`
- `src/pages/branch/inventory/InventoryPage.jsx`
- `src/pages/branch/orders/OrdersPage.jsx`
- `src/pages/branch/refunds/RefundsPage.jsx`
- `src/pages/branch/transactions/TransactionsPage.jsx`

### Store manager panel

- `src/pages/store/branches/BranchesPage.jsx`
- `src/pages/store/categories/CategoriesPage.jsx`
- `src/pages/store/employees/EmployeesPage.jsx`
- `src/pages/store/products/ProductsPage.jsx`
- `src/pages/store/sales/SalesPage.jsx`
- `src/pages/store/transactions/TransactionsPage.jsx`

### Super-admin panel

- `src/pages/super-admin/users/UsersPage.jsx`
- `src/pages/super-admin/stores/StoresPage.jsx`
- `src/pages/super-admin/pending-requests/PendingRequestsPage.jsx`
- `src/pages/super-admin/sales/SalesPage.jsx`
- `src/pages/super-admin/exports/ExportsPage.jsx`

Not every page needs identical behavior. The correct choice depends on the workflow:

- Management tables: URL-based pagination with page controls.
- Large picker/grid: ten records initially plus infinite scroll.
- Reports and exports: backend filtering and aggregation, not browser-side loading of all rows.
- Dashboard cards: summary endpoints, not full table queries.

## 29. Recommended next implementation order

1. Add shared backend `Page<T>` endpoints for products, customers, employees, branches, orders, refunds, and transactions.
2. Add consistent request helpers in `src/services/api.js`.
3. Convert the branch and store management tables first, because they are the most likely to grow.
4. Convert super-admin users, stores, and pending requests.
5. Replace combined transaction screens with a backend transaction feed that pages sales and refunds together.
6. Add aggregate summary endpoints for dashboard totals and report cards.
7. Add debounced search so every keystroke does not immediately send a request.
8. Add integration tests for page boundaries, sorting, search, empty results, and last-page behavior.

## 30. Advanced improvements after pagination

Once all panels use paginated endpoints, the next advanced improvements are:

- Debounce search by roughly 250–400 milliseconds.
- Cancel stale requests with `AbortController` when the user changes search quickly.
- Use stable secondary sorting, for example `createdAt,desc` followed by `id,desc`.
- Keep authorization checks in the backend service, never only in the frontend.
- Use database indexes for searchable and sortable columns.
- Return summary counts from dedicated aggregate queries.
- Cache stable catalog data carefully, while always refreshing inventory stock after checkout.
- Add virtualized rendering only if a single loaded page becomes visually large.
- Add API contract tests to ensure every page response contains `content`, `totalPages`, `totalElements`, `number`, and `last`.

The next concrete milestone is therefore: **paginate the branch, store-manager, and super-admin listing screens, then build one combined paginated transaction endpoint for sales and refunds.**
