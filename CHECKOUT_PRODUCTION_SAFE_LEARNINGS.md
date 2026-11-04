# Checkout hardening: what changed

This is the smallest checkout safety change. The UI and request shape were kept compatible.

## 1. The server now owns the price

Before, the service accepted the client price when it was present:

```java
Double itemPrice = itemDto.getPrice();
```

After, the price always comes from the stored product:

```java
Double itemPrice;
if (product.getPrice() != null) itemPrice = product.getPrice();
else if (product.getSellingPrice() != null) itemPrice = product.getSellingPrice();
else if (product.getMrp() != null) itemPrice = product.getMrp();
else throw new BadRequestException("Product price is missing");
```

Why: a browser can be modified. It must not be able to send `price: 1` for a product that costs 2,499.

## 2. Checkout is atomic

Before:

```java
public OrderDto createOrder(OrderDto orderDto) { ... }
```

After:

```java
@Transactional
public OrderDto createOrder(OrderDto orderDto) { ... }
```

Why: order creation and stock deduction now commit together. If one item fails, the earlier stock deductions roll back with the order.

## 3. The authenticated branch is used

The service already derives the branch and cashier from `userService.getCurrentUser()`. Client-supplied branch/cashier values are not trusted.

## 4. Branch inventory is the stock source

Before, checkout reduced `Product.quantity`.

After, it loads the inventory row for the authenticated branch:

```java
Inventory inventory = inventoryRepository
    .findByProductIdAndBranchIdForUpdate(productId, branch.getId())
    .orElseThrow(() -> new BadRequestException("Product is not stocked at this branch"));

if (inventory.getQuantity() < quantity) {
    throw new BadRequestException("Not enough stock");
}
inventory.setQuantity(inventory.getQuantity() - quantity);
inventoryRepository.save(inventory);
```

Why: the same product can have different quantities in different branches.

## 5. Row locking prevents overselling

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from Inventory i where i.product.id = :productId and i.branch.id = :branchId")
Optional<Inventory> findByProductIdAndBranchIdForUpdate(...);
```

Two checkout requests cannot both read and sell the same final unit. One request waits for the row lock, then sees the updated quantity and fails safely.

## 6. Invalid quantities and client status are ignored

Quantities must be positive, and checkout sets the successful order status on the server instead of trusting `orderDto.status`.

## Intentionally deferred

The project still uses `Double` in the existing order DTO/entity. Moving every monetary column to `BigDecimal` requires a database migration and changes across invoices, refunds, reports, and frontend formatting. That is the next separate change, not part of this minimal hardening step.

## Verification

- Backend compilation passed.
- No UI files were changed.
