package com.thoughtworks.cart.service;

import com.thoughtworks.cart.dto.InventoryDto;
import com.thoughtworks.cart.entity.Cart;
import com.thoughtworks.cart.entity.OrderItem;
import com.thoughtworks.cart.repository.CartRepository;
import com.thoughtworks.cart.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.lang.System.out;

@Service
public class CartService {

  @Autowired
  private CartRepository repository;
  @Autowired
  private OrderItemService orderItemService;
  @Autowired
  private RestUtil restUtil;

  public Cart getCartById(long id) {
    return repository
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException(format("Cart: %d does not exist", id)));
  }

  public Iterable<Cart> getAllCarts() {
    return repository.findAll();
  }

  @Transactional
  public Cart createCart(Cart cart) {
    if (cart.getId() > 0 && repository.findById(cart.getId()).isPresent()) {
      throw new EntityExistsException(format("Cart: %d already exists for Product: %d", cart.getId()));
    }
    final Set<OrderItem> orderItems = new HashSet<>();

    System.out.println("\n\n********** Initial size orderItems = " + cart.getOrderItems().size());

    cart.getOrderItems().forEach(orderItem -> {
      if (getInventory(orderItem.getProductId()).getCount() > orderItem.getCount()) {
        System.out.println("\norderItem added = " + orderItem.getProductId());
        orderItems.add(orderItem);
      }
    });
    System.out.println("\n\norderItems.size() = " + orderItems.size());

    cart.setOrderItems(null);
    final Cart savedCart = repository.save(cart);
    System.out.println("\n\nCart saved");

    orderItems.forEach(orderItem -> {
      orderItemService.createOrderItem(orderItem);
      System.out.println("\norderItem saved = " + orderItem.getProductId());
    });
    System.out.println("\n\n order items saved");
    return savedCart;
  }

  private InventoryDto getInventory(long productId) {
    InventoryDto inventoryDto = restUtil.get("http://localhost:2222/inventory/product/" + productId, InventoryDto.class);
    out.println("inventoryDto = " + inventoryDto);
    return inventoryDto;
  }

  public String deleteCart(int id) {
    repository.delete(getCartById(id));
    return format("Cart: %d Successfully deleted", id);
  }

  public Cart updateCart(final Cart cart) {
    //todo implement it
    return null;
  }
}

//  final long inventoryCount = getInventory(orderItem.getProductId()).getCount();
//  final long orderItemCount = orderItem.getCount();
//      if (inventoryCount < orderItemCount) {
//  System.out.println(format("Inventory has %d items but requested %d ", inventoryCount, orderItemCount));
//  }
