const itemInputs = [...document.querySelectorAll("[data-item-input]")];
const selectedItems = document.getElementById("selectedItems");
const itemCount = document.getElementById("itemCount");
const quantityCount = document.getElementById("quantityCount");
const subtotalValue = document.getElementById("subtotalValue");
const discountValue = document.getElementById("discountValue");
const gstValue = document.getElementById("gstValue");
const grandTotalValue = document.getElementById("grandTotalValue");
const discountInput = document.getElementById("discountInput");
const productSearch = document.getElementById("productSearch");
const operatorName = document.getElementById("operatorName");
const operatorField = document.getElementById("operatorField");
const billNumber = document.getElementById("billNumber");
const billNoField = document.getElementById("billNoField");
const currentDate = document.getElementById("currentDate");
const billingForm = document.getElementById("billingForm");
const demoFill = document.getElementById("demoFill");

const formatter = new Intl.NumberFormat("en-IN", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function formatCurrency(value) {
  return `Rs. ${formatter.format(value)}`;
}

function syncHeaderData() {
  const params = new URLSearchParams(window.location.search);
  const username = params.get("username") || "Cashier";
  const normalizedName = username.trim() || "Cashier";
  const billId = `FM-${String(Math.floor(Math.random() * 9000) + 1000)}`;
  const today = new Date().toLocaleDateString("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });

  operatorName.textContent = normalizedName;
  operatorField.value = normalizedName;
  billNumber.textContent = billId;
  billNoField.value = billId;
  currentDate.textContent = today;
}

function updateSummary() {
  let subtotal = 0;
  let distinctItems = 0;
  let totalQuantity = 0;
  const rows = [];

  itemInputs.forEach((input) => {
    const quantity = Number(input.value) || 0;
    const price = Number(input.dataset.price) || 0;
    if (quantity > 0) {
      const name = input.name.charAt(0).toUpperCase() + input.name.slice(1);
      const lineTotal = quantity * price;
      subtotal += lineTotal;
      distinctItems += 1;
      totalQuantity += quantity;
      rows.push({ name, quantity, lineTotal });
    }
  });

  const discountPercent = Math.min(50, Math.max(0, Number(discountInput.value) || 0));
  const discountAmount = subtotal * (discountPercent / 100);
  const taxableAmount = subtotal - discountAmount;
  const gstAmount = taxableAmount * 0.05;
  const grandTotal = taxableAmount + gstAmount;

  itemCount.textContent = distinctItems;
  quantityCount.textContent = totalQuantity;
  subtotalValue.textContent = formatCurrency(subtotal);
  discountValue.textContent = formatCurrency(discountAmount);
  gstValue.textContent = formatCurrency(gstAmount);
  grandTotalValue.textContent = formatCurrency(grandTotal);

  if (!rows.length) {
    selectedItems.innerHTML = '<p class="empty-state">No items selected yet.</p>';
    return;
  }

  selectedItems.innerHTML = rows
    .map(
      (row) => `
        <div class="selected-item">
          <div>
            <span>${row.name}</span>
            <strong>${row.quantity} unit(s)</strong>
          </div>
          <strong>${formatCurrency(row.lineTotal)}</strong>
        </div>
      `
    )
    .join("");
}

function setupSearch() {
  const itemRows = [...document.querySelectorAll("[data-item-row]")];
  productSearch.addEventListener("input", () => {
    const query = productSearch.value.trim().toLowerCase();
    itemRows.forEach((row) => {
      const itemName = row.dataset.name || "";
      row.style.display = itemName.includes(query) ? "" : "none";
    });
  });
}

function setupDemoFill() {
  demoFill.addEventListener("click", () => {
    billingForm.customerName.value = "Ananya Sharma";
    billingForm.phone.value = "9876543210";
    billingForm.paymentMethod.value = "UPI";
    billingForm.discount.value = 8;
    billingForm.milk.value = 2;
    billingForm.bread.value = 1;
    billingForm.butter.value = 1;
    billingForm.cheese.value = 1;
    billingForm.cake.value = 0;
    billingForm.tea.value = 3;
    billingForm.coffee.value = 1;
    billingForm.rice.value = 2;
    updateSummary();
  });
}

itemInputs.forEach((input) => input.addEventListener("input", updateSummary));
discountInput.addEventListener("input", updateSummary);
document.getElementById("resetForm").addEventListener("click", () => {
  window.setTimeout(updateSummary, 0);
});

syncHeaderData();
setupSearch();
setupDemoFill();
updateSummary();
