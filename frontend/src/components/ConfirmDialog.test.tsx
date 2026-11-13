import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { expect, it, vi } from "vitest"
import { ConfirmDialog } from "./ConfirmDialog"

it("exige confirmación escrita y conserva el diálogo si la operación falla", async () => {
  const confirm = vi.fn().mockRejectedValue(new Error("fallo de prueba"))
  render(<ConfirmDialog trigger={<button>Eliminar</button>} title="Confirmar eliminación" requireText="ELIMINAR" onConfirm={confirm} />)
  await userEvent.click(screen.getByRole("button", { name: "Eliminar" }))
  expect(screen.getByRole("button", { name: "Confirmar" })).toBeDisabled()
  await userEvent.type(screen.getByRole("textbox"), "ELIMINAR")
  await userEvent.click(screen.getByRole("button", { name: "Confirmar" }))
  expect(await screen.findByRole("alert")).toHaveTextContent("No se pudo completar")
  expect(screen.getByRole("dialog")).toBeInTheDocument()
  expect(confirm).toHaveBeenCalledTimes(1)
})
